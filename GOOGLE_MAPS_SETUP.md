# Google Maps API Setup Guide

This guide will help you set up Google Maps API for the location feature in Tummy Go!.

## Step 1: Get Google Maps API Key

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Create a new project or select an existing one
3. Enable the following APIs:
   - **Maps JavaScript API** (for displaying the map)
   - **Places API** (for address autocomplete and geocoding)
   - **Geocoding API** (for converting addresses to coordinates and vice versa)

## Step 2: Create API Key

1. Navigate to **APIs & Services** > **Credentials**
2. Click **Create Credentials** > **API Key**
3. Copy your API key
4. (Recommended) Restrict the API key:
   - Click on the API key to edit it
   - Under **Application restrictions**, select **HTTP referrers (web sites)**
   - Add your domain (e.g., `http://localhost:8080/*`, `https://yourdomain.com/*`)
   - Under **API restrictions**, select **Restrict key** and choose:
     - Maps JavaScript API
     - Places API
     - Geocoding API

## Step 3: Configure in Application

1. Open `src/main/resources/application.properties`
2. Find the line: `google.maps.api.key=YOUR_GOOGLE_MAPS_API_KEY`
3. Replace `YOUR_GOOGLE_MAPS_API_KEY` with your actual API key

Example:
```properties
google.maps.api.key=AIzaSyBxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

## Step 4: Test the Integration

1. Start your Spring Boot application
2. Log in as a customer
3. Add items to cart
4. Go to the cart page
5. You should see:
   - A search box for location
   - An interactive Google Map
   - Ability to click on the map or search for locations
   - Selected location will be saved

## Features

- **Search Location**: Type in the search box to get autocomplete suggestions
- **Click on Map**: Click anywhere on the map to set your location
- **Drag Marker**: Drag the marker to fine-tune your location
- **Current Location**: The map will try to detect your current location automatically
- **Saved Location**: Your previously saved location will be loaded automatically

## Troubleshooting

### Map Not Showing
- Check if your API key is correct in `application.properties`
- Verify that Maps JavaScript API is enabled
- Check browser console for errors
- Ensure your API key restrictions allow your domain

### Autocomplete Not Working
- Verify that Places API is enabled
- Check that your API key has Places API access

### Location Not Saving
- Check browser console for JavaScript errors
- Verify the form is submitting correctly
- Check server logs for any errors

## Cost Considerations

Google Maps API has a free tier:
- Maps JavaScript API: $200 free credit per month
- Places API: $200 free credit per month
- Geocoding API: $200 free credit per month

For most small to medium applications, the free tier should be sufficient. Monitor your usage in Google Cloud Console.

## Security Best Practices

1. **Restrict API Key**: Always restrict your API key to specific domains
2. **Use Environment Variables**: Consider using environment variables for production
3. **Monitor Usage**: Set up billing alerts in Google Cloud Console
4. **Rotate Keys**: Regularly rotate your API keys for security

