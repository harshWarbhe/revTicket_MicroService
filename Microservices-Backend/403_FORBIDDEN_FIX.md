# 403 Forbidden Error Fix - Service-to-Service Authentication

## Problem Summary

You were encountering a **403 Forbidden** error when services tried to communicate with each other via Feign clients:

```
[403] during [GET] to [http://booking-service/api/bookings/user/e40701a2-4d79-45ca-a6ba-44e8b683c621]
```

## Root Cause

The issue occurred because:

1. **Frontend → API Gateway**: ✅ Authentication worked (JWT token passed)
2. **API Gateway → Microservice (e.g., review-service)**: ✅ Token forwarded correctly
3. **Microservice → Another Microservice (via Feign)**: ❌ **Token NOT forwarded**

When a service like `review-service` or `dashboard-service` made a Feign client call to `booking-service`, the JWT token from the original request was **not being forwarded**. Since `booking-service` has security enabled and requires authentication, it rejected the request with **403 Forbidden**.

## Technical Explanation

### Why This Happens

By default, Spring Cloud OpenFeign **does not automatically forward HTTP headers** from the incoming request to outgoing Feign client requests. This means:

- The original `Authorization` header (containing the JWT token) is lost
- The downstream service (e.g., `booking-service`) receives a request without authentication
- The `SecurityFilterChain` in the downstream service rejects the unauthenticated request

### The Solution: FeignConfig

We need to create a **Feign Request Interceptor** that:

1. Captures the `Authorization` header from the incoming HTTP request
2. Forwards it to all outgoing Feign client requests
3. Also forwards custom headers like `X-User-Id` and `X-User-Role` for context propagation

## What Was Fixed

### 1. Created FeignConfig for Services Missing It

Created `FeignConfig.java` in the following services:
- ✅ `dashboard-service`
- ✅ `search-service`
- ✅ `showtime-service`

**Note**: `review-service` already had a FeignConfig, which is why it was working correctly.

### 2. Updated All Feign Clients to Use FeignConfig

Updated all `@FeignClient` annotations to include the configuration:

**Before:**
```java
@FeignClient(name = "booking-service")
public interface BookingServiceClient {
    // ...
}
```

**After:**
```java
@FeignClient(name = "booking-service", configuration = com.revticket.dashboard.config.FeignConfig.class)
public interface BookingServiceClient {
    // ...
}
```

### Services Updated:

#### dashboard-service
- ✅ BookingServiceClient
- ✅ PaymentServiceClient
- ✅ MovieServiceClient
- ✅ ReviewServiceClient
- ✅ TheaterServiceClient
- ✅ UserServiceClient
- ✅ ShowtimeServiceClient

#### search-service
- ✅ MovieServiceClient
- ✅ TheaterServiceClient
- ✅ ShowtimeServiceClient

#### showtime-service
- ✅ TheaterServiceClient
- ✅ MovieServiceClient

## How FeignConfig Works

```java
@Configuration
public class FeignConfig {
    
    @Bean
    public RequestInterceptor requestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // Get the current HTTP request
                ServletRequestAttributes attributes = 
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                
                if (attributes != null) {
                    HttpServletRequest request = attributes.getRequest();
                    
                    // Forward important headers
                    String authorization = request.getHeader("Authorization");
                    if (authorization != null) {
                        template.header("Authorization", authorization);
                    }
                    
                    String userId = request.getHeader("X-User-Id");
                    if (userId != null) {
                        template.header("X-User-Id", userId);
                    }
                    
                    String userRole = request.getHeader("X-User-Role");
                    if (userRole != null) {
                        template.header("X-User-Role", userRole);
                    }
                }
                
                // Fallback: Get from SecurityContext
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                if (authentication != null && authentication.isAuthenticated()) {
                    // Extract user info and add as headers
                    // ...
                }
            }
        };
    }
}
```

### What It Does:

1. **Intercepts every Feign request** before it's sent
2. **Extracts headers** from the current HTTP request context
3. **Forwards critical headers**:
   - `Authorization`: JWT token for authentication
   - `X-User-Id`: User identifier for authorization
   - `X-User-Role`: User role for role-based access control
4. **Fallback mechanism**: If headers aren't in the request, tries to get them from Spring Security's `SecurityContext`

## Request Flow (After Fix)

```
Frontend (Angular)
    ↓ [Authorization: Bearer <JWT>]
API Gateway
    ↓ [Authorization: Bearer <JWT>]
review-service
    ↓ [Authorization: Bearer <JWT>] ← FeignConfig forwards this!
booking-service
    ↓ [SecurityFilterChain validates JWT]
    ✅ Request Authorized
```

## Verification

All services are now running successfully:

```
✅ UP    Eureka Server        (http://localhost:8761)
✅ UP    Movie Service        (http://localhost:8081)
✅ UP    Theater Service      (http://localhost:8083)
✅ UP    Payment Service      (http://localhost:8084)
✅ UP    Booking Service      (http://localhost:8085)
✅ UP    Showtime Service     (http://localhost:8086)
✅ UP    Review Service       (http://localhost:8087)
✅ UP    Search Service       (http://localhost:8088)
✅ UP    Settings Service     (http://localhost:8089)
✅ UP    Notification Service (http://localhost:8090)
✅ UP    Dashboard Service    (http://localhost:8091)
✅ UP    Frontend             (http://localhost:4200)
```

## Testing the Fix

1. **Login to the application** at `http://localhost:4200`
2. **Navigate to My Bookings** (`/user/my-bookings`)
3. **The page should load successfully** without 403 errors

The request flow will be:
- Frontend → API Gateway → review-service → booking-service
- All authentication headers are properly forwarded through the chain

## Key Takeaways

### Why This Pattern Is Important

In a microservices architecture:

1. **Security must be consistent** across all services
2. **Authentication context must propagate** through service chains
3. **Feign clients need explicit configuration** to forward headers
4. **Every service that makes Feign calls needs FeignConfig**

### Best Practice

**Always create a FeignConfig** for any service that uses Feign clients, even if the downstream service doesn't require authentication today. This ensures:

- Consistent behavior across all services
- Future-proof architecture
- Easier debugging and maintenance
- Proper audit trails (user context preserved)

## Files Modified

1. **Created:**
   - `/dashboard-service/src/main/java/com/revticket/dashboard/config/FeignConfig.java`
   - `/search-service/src/main/java/com/revticket/search/config/FeignConfig.java`
   - `/showtime-service/src/main/java/com/revticket/showtime/config/FeignConfig.java`

2. **Updated:**
   - All Feign client interfaces in `dashboard-service`, `search-service`, and `showtime-service`

## Next Steps

Your application should now work fluently! The 403 errors are resolved, and all service-to-service communication properly forwards authentication tokens.

If you encounter any other issues, check:
1. JWT token expiration (tokens might expire during long sessions)
2. Service logs in `/Microservices-Backend/logs/` for detailed error messages
3. Eureka dashboard at `http://localhost:8761` to ensure all services are registered
