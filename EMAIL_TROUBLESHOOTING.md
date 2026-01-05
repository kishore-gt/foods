# Email Troubleshooting Guide

## Changes Made to Fix Email Issues

### 1. Enhanced Logging
- Added detailed logging at every step of email sending
- Logs now show:
  - When email sending is attempted
  - Recipient email address
  - Order/MultiOrder ID
  - Success/failure messages
  - Specific error types

### 2. Enabled Mail Debug
- Set `spring.mail.properties.mail.debug=true` in `application.properties`
- This will show detailed SMTP communication in logs

### 3. Improved Error Handling
- Added null checks for User, Order, and MultiOrder objects
- Enhanced error messages with specific failure reasons
- Added SSL trust configuration for Gmail

### 4. Configuration Updates
- Increased timeout values (10 seconds)
- Added SSL trust for smtp.gmail.com
- Enabled mail debug mode

## How to Check if Emails Are Being Sent

### Step 1: Check Application Logs
When you place an order, look for these log messages:

**If JavaMailSender is NULL:**
```
ERROR: JavaMailSender is NULL! Email functionality will not work.
```

**If email sending is attempted:**
```
=== ATTEMPTING TO SEND MULTIORDER CONFIRMATION EMAIL ===
To: customer@example.com
MultiOrder ID: 123
```

**If email is sent successfully:**
```
✅ EMAIL SENT SUCCESSFULLY to customer@example.com
Email Subject: Order Confirmation - Tummy Go!
```

**If there's an error:**
```
ERROR: Email authentication failed. Please check your email credentials...
```
or
```
ERROR: Failed to send email to customer@example.com. Check SMTP settings...
```

### Step 2: Check Mail Debug Output
With `spring.mail.properties.mail.debug=true`, you'll see detailed SMTP logs:
- Connection attempts
- Authentication process
- Message sending details
- Any errors from the SMTP server

### Step 3: Verify Gmail App Password
1. Go to https://myaccount.google.com/apppasswords
2. Verify the app password matches what's in `application.properties`
3. Ensure 2-Step Verification is enabled

### Step 4: Test Email Configuration
The application will log during startup:
```
EmailService initialized successfully with JavaMailSender
```
If you see:
```
ERROR: JavaMailSender is NULL!
```
Then Spring Boot couldn't auto-configure the mail sender. Check:
- `spring-boot-starter-mail` dependency in `pom.xml`
- Email properties in `application.properties`

## Common Issues and Solutions

### Issue 1: JavaMailSender is NULL
**Solution:**
- Check if `spring-boot-starter-mail` is in `pom.xml`
- Verify email properties in `application.properties`
- Restart the application

### Issue 2: Authentication Failed
**Solution:**
- Verify Gmail app password is correct (no spaces)
- Ensure 2-Step Verification is enabled
- Check if the password in `application.properties` matches the app password

### Issue 3: Connection Timeout
**Solution:**
- Check internet connection
- Verify firewall isn't blocking SMTP port 587
- Try increasing timeout values in `application.properties`

### Issue 4: Emails Not Received
**Solution:**
- Check spam/junk folder
- Verify recipient email address is correct
- Check if Gmail has blocked the sender
- Look for error messages in application logs

## Current Email Configuration

```properties
spring.mail.host=smtp.gmail.com
spring.mail.port=587
spring.mail.username=kishoremovva11@gmail.com
spring.mail.password=abhyuoqozpbogdxx
spring.mail.properties.mail.smtp.auth=true
spring.mail.properties.mail.smtp.starttls.enable=true
spring.mail.properties.mail.smtp.starttls.required=true
spring.mail.properties.mail.smtp.connectiontimeout=10000
spring.mail.properties.mail.smtp.timeout=10000
spring.mail.properties.mail.smtp.writetimeout=10000
spring.mail.properties.mail.debug=true
spring.mail.properties.mail.smtp.ssl.trust=smtp.gmail.com
```

## Next Steps

1. **Restart the application** to apply the new configuration
2. **Place a test order** and watch the logs
3. **Check for log messages** indicating email sending attempts
4. **Verify the recipient email** receives the email
5. **Check spam folder** if email is not in inbox

## Email Methods Available

All email methods now have enhanced logging:
- `sendOrderConfirmationEmail()` - For order confirmations
- `sendMultiOrderConfirmationEmail()` - For MultiOrder confirmations
- `sendMultiOrderStatusUpdateEmail()` - For status updates
- `sendPreorderApprovalEmail()` - For preorder approvals
- `sendPreorderRejectionEmail()` - For preorder rejections

Each method will log detailed information about the sending process.

