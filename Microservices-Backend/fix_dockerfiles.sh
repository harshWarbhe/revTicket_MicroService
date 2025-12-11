#!/bin/bash

# Fix dockerfile paths for each service
sed -i '82s|dockerfile: .*|dockerfile: ./user-service/Dockerfile|' docker-compose.yml
sed -i '114s|dockerfile: .*|dockerfile: ./movie-service/Dockerfile|' docker-compose.yml  
sed -i '140s|dockerfile: .*|dockerfile: ./theater-service/Dockerfile|' docker-compose.yml
sed -i '166s|dockerfile: .*|dockerfile: ./showtime-service/Dockerfile|' docker-compose.yml
sed -i '198s|dockerfile: .*|dockerfile: ./booking-service/Dockerfile|' docker-compose.yml
sed -i '232s|dockerfile: .*|dockerfile: ./payment-service/Dockerfile|' docker-compose.yml
sed -i '260s|dockerfile: .*|dockerfile: ./review-service/Dockerfile|' docker-compose.yml
sed -i '285s|dockerfile: .*|dockerfile: ./search-service/Dockerfile|' docker-compose.yml
sed -i '312s|dockerfile: .*|dockerfile: ./notification-service/Dockerfile|' docker-compose.yml
sed -i '338s|dockerfile: .*|dockerfile: ./settings-service/Dockerfile|' docker-compose.yml
sed -i '364s|dockerfile: .*|dockerfile: ./dashboard-service/Dockerfile|' docker-compose.yml
