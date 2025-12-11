import { Component, OnInit, signal, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { BookingDraft, BookingCostBreakdown } from '../../../core/models/booking.model';
import { BookingService } from '../../../core/services/booking.service';
import { AuthService } from '../../../core/services/auth.service';
import { AlertService } from '../../../core/services/alert.service';
import { SettingsService } from '../../../core/services/settings.service';
import { RazorpayService } from '../../../core/services/razorpay.service';
import { finalize } from 'rxjs/operators';

declare var Razorpay: any;

@Component({
  selector: 'app-payment',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, FormsModule, RouterLink],
  templateUrl: './payment.component.html',
  styleUrls: ['./payment.component.css']
})
export class PaymentComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private bookingService = inject(BookingService);
  private authService = inject(AuthService);
  private alertService = inject(AlertService);
  private razorpayService = inject(RazorpayService);
  readonly settingsService = inject(SettingsService);

  contactForm: FormGroup;
  processing = signal(false);
  bookingDraft = signal<BookingDraft | null>(null);
  costBreakdown = signal<BookingCostBreakdown | undefined>(undefined);
  showtimeId = '';
  razorpayLoaded = signal(false);

  constructor() {
    this.contactForm = this.fb.group({
      name: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      phone: ['', [Validators.required, Validators.pattern(/^\d{10}$/)]]
    });
  }

  async ngOnInit(): Promise<void> {
    window.scrollTo(0, 0);
    
    const showtimeId = this.route.snapshot.paramMap.get('showtimeId');
    if (!showtimeId) {
      this.alertService.error('Invalid booking session.');
      this.router.navigate(['/user/home']);
      return;
    }
    
    this.showtimeId = showtimeId;
    const draft = this.bookingService.getCurrentBooking();
    
    if (!draft || draft.showtimeId !== showtimeId) {
      this.alertService.error('Booking session expired. Please select seats again.');
      this.router.navigate(['/user/seat-booking', showtimeId]);
      return;
    }
    
    this.bookingDraft.set(draft);
    this.costBreakdown.set(this.bookingService.calculateCostBreakdown(draft.totalAmount));
    
    const currentUser = this.authService.getCurrentUser();
    if (currentUser) {
      this.contactForm.patchValue({
        name: currentUser.name,
        email: currentUser.email,
        phone: currentUser.phone || ''
      });
    }

    // Load Razorpay script
    const loaded = await this.razorpayService.loadRazorpayScript();
    this.razorpayLoaded.set(loaded);
    if (!loaded) {
      this.alertService.error('Failed to load payment gateway. Please refresh.');
    }
  }

  processPayment(): void {
    const draft = this.bookingDraft();
    const breakdown = this.costBreakdown();
    
    if (!draft || !breakdown) {
      this.alertService.error('Booking information missing.');
      this.router.navigate(['/user/home']);
      return;
    }

    if (!this.contactForm.valid) {
      this.contactForm.markAllAsTouched();
      this.alertService.error('Please fill in all contact details.');
      return;
    }

    if (!this.razorpayLoaded()) {
      this.alertService.error('Payment gateway not loaded. Please refresh.');
      return;
    }

    this.processing.set(true);
    const contact = this.contactForm.value;

    // Create Razorpay order
    this.razorpayService.createOrder({
      amount: breakdown.total,
      showtimeId: draft.showtimeId,
      currency: 'INR'
    }).pipe(finalize(() => this.processing.set(false)))
      .subscribe({
        next: (orderResponse) => {
          this.openRazorpayCheckout(orderResponse, draft, breakdown, contact);
        },
        error: (err) => {
          console.error('Payment order creation failed:', err);
          const errorMsg = err.error?.error || err.message || 'Failed to create payment order. Please try again.';
          this.alertService.error(errorMsg);
        }
      });
  }

  private openRazorpayCheckout(orderResponse: any, draft: BookingDraft, breakdown: BookingCostBreakdown, contact: any): void {
    const options = {
      key: orderResponse.key,
      amount: orderResponse.amount,
      currency: orderResponse.currency,
      name: 'RevTicket',
      description: `${draft.movieTitle} - ${draft.seatLabels?.join(', ') || draft.seats.join(', ')}`,
      order_id: orderResponse.orderId,
      prefill: {
        name: contact.name,
        email: contact.email,
        contact: contact.phone
      },
      theme: {
        color: '#e50914'
      },
      handler: (response: any) => {
        this.handlePaymentSuccess(response, draft, breakdown, contact);
      },
      modal: {
        ondismiss: () => {
          this.alertService.error('Payment cancelled');
        }
      }
    };

    const razorpay = new Razorpay(options);
    razorpay.on('payment.failed', (response: any) => {
      this.handlePaymentFailure(response, draft, contact);
    });
    razorpay.open();
  }

  private handlePaymentSuccess(response: any, draft: BookingDraft, breakdown: BookingCostBreakdown, contact: any): void {
    this.processing.set(true);

    if (!response.razorpay_order_id || !response.razorpay_payment_id || !response.razorpay_signature) {
      this.processing.set(false);
      this.alertService.error('Payment response incomplete. Please try again.');
      return;
    }

    if (!draft.showtimeId || !draft.seats || draft.seats.length === 0) {
      this.processing.set(false);
      this.alertService.error('Booking information incomplete. Please start over.');
      return;
    }

    if (!contact.name || !contact.email || !contact.phone) {
      this.processing.set(false);
      this.alertService.error('Contact information incomplete.');
      return;
    }

    const verificationRequest = {
      razorpayOrderId: response.razorpay_order_id,
      razorpayPaymentId: response.razorpay_payment_id,
      razorpaySignature: response.razorpay_signature,
      showtimeId: draft.showtimeId,
      seats: draft.seats,
      seatLabels: draft.seatLabels || draft.seats,
      totalAmount: breakdown.total,
      customerName: contact.name,
      customerEmail: contact.email,
      customerPhone: contact.phone
    };

    this.razorpayService.verifyPayment(verificationRequest)
      .pipe(finalize(() => this.processing.set(false)))
      .subscribe({
        next: (result) => {
          this.bookingService.clearCurrentBooking();
          console.log('Payment verified successfully:', result);
          this.alertService.success('Payment successful! Redirecting...');
          this.router.navigate(['/user/payment-success'], {
            state: {
              bookingId: result.bookingId,
              ticketNumber: result.ticketNumber,
              draft: draft,
              amount: breakdown.total
            }
          });
        },
        error: (err) => {
          console.error('Payment verification failed:', err);
          console.error('Error details:', {
            status: err.status,
            statusText: err.statusText,
            error: err.error,
            message: err.message,
            url: err.url
          });
          
          let errorMsg = 'Payment verification failed. Please try again.';
          
          // Handle validation errors with field-level details
          if (err.status === 400) {
            if (err.error?.errors && typeof err.error.errors === 'object') {
              // Build detailed error message from field-level errors
              const fieldErrors = Object.entries(err.error.errors)
                .map(([field, message]) => `${field}: ${message}`)
                .join(' | ');
              errorMsg = `Validation failed: ${fieldErrors}`;
            } else if (err.error?.message) {
              errorMsg = err.error.message;
            } else if (err.error?.error) {
              errorMsg = err.error.error;
            } else {
              errorMsg = 'Invalid payment verification request.';
            }
          } else if (err.status === 401) {
            errorMsg = 'Authentication failed. Please login again.';
          } else if (err.status === 403) {
            errorMsg = 'Access denied. Please check your permissions.';
          } else if (err.error?.message) {
            errorMsg = err.error.message;
          } else if (err.error?.error) {
            errorMsg = err.error.error;
          } else if (err.message) {
            errorMsg = err.message;
          }
          
          this.alertService.error(errorMsg);
          this.router.navigate(['/user/payment-failure'], {
            state: { draft: draft }
          });
        }
      });
  }

  private handlePaymentFailure(response: any, draft: BookingDraft, contact: any): void {
    this.alertService.error('Payment failed. Please try again.');
    this.router.navigate(['/user/payment-failure'], {
      state: { draft: draft }
    });
  }
  
  private createSlug(title: string): string {
    return title.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '');
  }

  goBack(): void {
    if (this.showtimeId) {
      this.router.navigate(['/user/seat-booking', this.showtimeId]);
    } else {
      this.router.navigate(['/user/home']);
    }
  }
}