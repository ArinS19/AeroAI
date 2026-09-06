-- V2__seed_airspaces.sql
-- Seed catalogue of major city airspaces (India-focused, with strong
-- international coverage for search usefulness). Coordinates are real
-- city-center coordinates; bounding boxes are approximate metro-area
-- viewports sized for Leaflet-style map display.

INSERT INTO airspaces
    (city_name, country, latitude, longitude, default_zoom, lamin, lomin, lamax, lomax, display_region, aliases, slug)
VALUES
    -- ===================== India =====================
    ('Mumbai', 'India', 19.0760, 72.8777, 9, 17.28, 71.08, 20.88, 74.68, 'Maharashtra, India', 'Bombay', 'mumbai'),
    ('New Delhi', 'India', 28.6139, 77.2090, 9, 26.81, 75.41, 30.41, 79.01, 'Delhi, India', 'Delhi', 'new-delhi'),
    ('Bengaluru', 'India', 12.9716, 77.5946, 10, 11.67, 76.29, 14.27, 78.89, 'Karnataka, India', 'Bangalore', 'bengaluru'),
    ('Hyderabad', 'India', 17.3850, 78.4867, 10, 16.09, 77.19, 18.69, 79.79, 'Telangana, India', NULL, 'hyderabad'),
    ('Chennai', 'India', 13.0827, 80.2707, 10, 11.78, 78.97, 14.38, 81.57, 'Tamil Nadu, India', 'Madras', 'chennai'),
    ('Kolkata', 'India', 22.5726, 88.3639, 10, 21.27, 87.06, 23.87, 89.66, 'West Bengal, India', 'Calcutta', 'kolkata'),
    ('Ahmedabad', 'India', 23.0225, 72.5714, 10, 21.82, 71.37, 24.22, 73.77, 'Gujarat, India', NULL, 'ahmedabad'),
    ('Pune', 'India', 18.5204, 73.8567, 10, 17.32, 72.66, 19.72, 75.06, 'Maharashtra, India', 'Poona', 'pune'),
    ('Jaipur', 'India', 26.9124, 75.7873, 10, 25.71, 74.59, 28.11, 76.99, 'Rajasthan, India', 'Pink City', 'jaipur'),
    ('Lucknow', 'India', 26.8467, 80.9462, 10, 25.65, 79.75, 28.05, 82.15, 'Uttar Pradesh, India', NULL, 'lucknow'),
    ('Kochi', 'India', 9.9312, 76.2673, 10, 8.93, 75.27, 10.93, 77.27, 'Kerala, India', 'Cochin', 'kochi'),
    ('Goa', 'India', 15.4909, 73.8278, 10, 14.49, 72.83, 16.49, 74.83, 'Goa, India', 'Panaji, Panjim', 'goa'),
    ('Chandigarh', 'India', 30.7333, 76.7794, 10, 29.73, 75.78, 31.73, 77.78, 'Punjab & Haryana, India', NULL, 'chandigarh'),
    ('Guwahati', 'India', 26.1445, 91.7362, 10, 25.14, 90.74, 27.14, 92.74, 'Assam, India', NULL, 'guwahati'),
    ('Nagpur', 'India', 21.1458, 79.0882, 10, 20.15, 78.09, 22.15, 80.09, 'Maharashtra, India', NULL, 'nagpur'),
    ('Indore', 'India', 22.7196, 75.8577, 10, 21.72, 74.86, 23.72, 76.86, 'Madhya Pradesh, India', NULL, 'indore'),
    ('Bhopal', 'India', 23.2599, 77.4126, 10, 22.26, 76.41, 24.26, 78.41, 'Madhya Pradesh, India', NULL, 'bhopal'),
    ('Coimbatore', 'India', 11.0168, 76.9558, 10, 10.02, 75.96, 12.02, 77.96, 'Tamil Nadu, India', 'Kovai', 'coimbatore'),
    ('Thiruvananthapuram', 'India', 8.5241, 76.9366, 10, 7.52, 75.94, 9.52, 77.94, 'Kerala, India', 'Trivandrum', 'thiruvananthapuram'),
    ('Amritsar', 'India', 31.6340, 74.8723, 10, 30.63, 73.87, 32.63, 75.87, 'Punjab, India', NULL, 'amritsar'),
    ('Varanasi', 'India', 25.3176, 82.9739, 10, 24.32, 81.97, 26.32, 83.97, 'Uttar Pradesh, India', 'Benares, Kashi', 'varanasi'),
    ('Patna', 'India', 25.5941, 85.1376, 10, 24.59, 84.14, 26.59, 86.14, 'Bihar, India', NULL, 'patna'),
    ('Bhubaneswar', 'India', 20.2961, 85.8245, 10, 19.30, 84.82, 21.30, 86.82, 'Odisha, India', NULL, 'bhubaneswar'),
    ('Visakhapatnam', 'India', 17.6868, 83.2185, 10, 16.69, 82.22, 18.69, 84.22, 'Andhra Pradesh, India', 'Vizag', 'visakhapatnam'),
    ('Surat', 'India', 21.1702, 72.8311, 10, 20.17, 71.83, 22.17, 73.83, 'Gujarat, India', NULL, 'surat'),

    -- ================= Middle East =================
    ('Dubai', 'United Arab Emirates', 25.2048, 55.2708, 10, 24.20, 54.27, 26.20, 56.27, 'UAE', NULL, 'dubai'),
    ('Abu Dhabi', 'United Arab Emirates', 24.4539, 54.3773, 10, 23.45, 53.38, 25.45, 55.38, 'UAE', NULL, 'abu-dhabi'),
    ('Doha', 'Qatar', 25.2854, 51.5310, 10, 24.29, 50.53, 26.29, 52.53, 'Qatar', NULL, 'doha'),
    ('Riyadh', 'Saudi Arabia', 24.7136, 46.6753, 10, 23.41, 45.38, 26.01, 47.98, 'Saudi Arabia', NULL, 'riyadh'),
    ('Jeddah', 'Saudi Arabia', 21.4858, 39.1925, 10, 20.29, 37.99, 22.69, 40.39, 'Makkah Province, Saudi Arabia', NULL, 'jeddah'),
    ('Muscat', 'Oman', 23.5880, 58.3829, 10, 22.59, 57.38, 24.59, 59.38, 'Oman', NULL, 'muscat'),

    -- ============= South Asia (rest) =============
    ('Colombo', 'Sri Lanka', 6.9271, 79.8612, 10, 5.93, 78.86, 7.93, 80.86, 'Sri Lanka', NULL, 'colombo'),
    ('Kathmandu', 'Nepal', 27.7172, 85.3240, 10, 26.72, 84.32, 28.72, 86.32, 'Nepal', NULL, 'kathmandu'),
    ('Dhaka', 'Bangladesh', 23.8103, 90.4125, 10, 22.61, 89.21, 25.01, 91.61, 'Bangladesh', NULL, 'dhaka'),
    ('Karachi', 'Pakistan', 24.8607, 67.0011, 10, 23.56, 65.70, 26.16, 68.30, 'Sindh, Pakistan', NULL, 'karachi'),
    ('Islamabad', 'Pakistan', 33.6844, 73.0479, 10, 32.68, 72.05, 34.68, 74.05, 'Pakistan', NULL, 'islamabad'),

    -- ============= Southeast & East Asia =============
    ('Singapore', 'Singapore', 1.3521, 103.8198, 10, 0.55, 103.02, 2.15, 104.62, 'Singapore', NULL, 'singapore'),
    ('Bangkok', 'Thailand', 13.7563, 100.5018, 10, 12.46, 99.20, 15.06, 101.80, 'Thailand', 'Krung Thep', 'bangkok'),
    ('Kuala Lumpur', 'Malaysia', 3.1390, 101.6869, 10, 1.94, 100.49, 4.34, 102.89, 'Malaysia', 'KL', 'kuala-lumpur'),
    ('Hong Kong', 'China', 22.3193, 114.1694, 10, 21.32, 113.17, 23.32, 115.17, 'Hong Kong SAR, China', NULL, 'hong-kong'),
    ('Manila', 'Philippines', 14.5995, 120.9842, 10, 13.30, 119.68, 15.90, 122.28, 'Philippines', NULL, 'manila'),
    ('Jakarta', 'Indonesia', -6.2088, 106.8456, 9, -7.71, 105.35, -4.71, 108.35, 'Indonesia', NULL, 'jakarta'),
    ('Seoul', 'South Korea', 37.5665, 126.9780, 9, 36.07, 125.48, 39.07, 128.48, 'South Korea', NULL, 'seoul'),
    ('Beijing', 'China', 39.9042, 116.4074, 9, 37.90, 114.41, 41.90, 118.41, 'China', 'Peking', 'beijing'),
    ('Shanghai', 'China', 31.2304, 121.4737, 9, 29.53, 119.77, 32.93, 123.17, 'China', NULL, 'shanghai'),
    ('Tokyo', 'Japan', 35.6762, 139.6503, 9, 33.38, 137.35, 37.98, 141.95, 'Japan', NULL, 'tokyo'),

    -- ===================== Europe =====================
    ('London', 'United Kingdom', 51.5074, -0.1278, 9, 49.51, -2.13, 53.51, 1.87, 'Greater London, UK', NULL, 'london'),
    ('Paris', 'France', 48.8566, 2.3522, 9, 47.16, 0.65, 50.56, 4.05, 'Île-de-France, France', NULL, 'paris'),
    ('Frankfurt', 'Germany', 50.1109, 8.6821, 10, 48.81, 7.38, 51.41, 9.98, 'Hesse, Germany', NULL, 'frankfurt'),
    ('Amsterdam', 'Netherlands', 52.3676, 4.9041, 10, 51.17, 3.70, 53.57, 6.10, 'Netherlands', NULL, 'amsterdam'),
    ('Istanbul', 'Turkey', 41.0082, 28.9784, 9, 39.31, 27.28, 42.71, 30.68, 'Turkey', NULL, 'istanbul'),
    ('Moscow', 'Russia', 55.7558, 37.6173, 9, 53.76, 35.62, 57.76, 39.62, 'Russia', NULL, 'moscow'),

    -- =================== North America ===================
    ('New York', 'United States', 40.7128, -74.0060, 9, 38.71, -76.01, 42.71, -72.01, 'New York, USA', 'NYC', 'new-york'),
    ('Los Angeles', 'United States', 34.0522, -118.2437, 9, 31.85, -120.44, 36.25, -116.04, 'California, USA', 'LA', 'los-angeles'),
    ('Chicago', 'United States', 41.8781, -87.6298, 9, 40.08, -89.43, 43.68, -85.83, 'Illinois, USA', NULL, 'chicago'),
    ('San Francisco', 'United States', 37.7749, -122.4194, 10, 36.47, -123.72, 39.07, -121.12, 'California, USA', 'SF, Bay Area', 'san-francisco'),
    ('Toronto', 'Canada', 43.6532, -79.3832, 9, 42.15, -80.88, 45.15, -77.88, 'Ontario, Canada', NULL, 'toronto'),

    -- ===================== Other =====================
    ('Sydney', 'Australia', -33.8688, 151.2093, 9, -35.57, 149.51, -32.17, 152.91, 'New South Wales, Australia', NULL, 'sydney'),
    ('Cairo', 'Egypt', 30.0444, 31.2357, 9, 28.54, 29.74, 31.54, 32.74, 'Egypt', NULL, 'cairo');
