# Email Configuration Guide

## Email Configuration (Gmail)

To enable email sending, you need to configure your Gmail account in `application.properties`:

### Steps:

1. **Enable 2-Step Verification** on your Gmail account:
   - Go to https://myaccount.google.com/security
   - Enable 2-Step Verification

2. **Generate an App Password**:
   - Go to https://myaccount.google.com/apppasswords
   - Select "Mail" and "Other (Custom name)"
   - Enter "Tummy Go!" as the name
   - Copy the 16-character password generated

3. **Update `application.properties`**:
   ```properties
   spring.mail.username=your-email@gmail.com
   spring.mail.password=your-16-character-app-password
   ```

### Alternative Email Providers:

**For Outlook/Hotmail:**
```properties
spring.mail.host=smtp-mail.outlook.com
spring.mail.port=587
spring.mail.username=your-email@outlook.com
spring.mail.password=your-password
```

**For Yahoo:**
```properties
spring.mail.host=smtp.mail.yahoo.com
spring.mail.port=587
spring.mail.username=your-email@yahoo.com
spring.mail.password=your-app-password
```

---

## Troubleshooting

### Email Not Sending:

1. Check if Gmail app password is correct
2. Verify 2-Step Verification is enabled
3. Check application logs for error messages
4. Ensure firewall/antivirus isn't blocking SMTP

### General Issues:

1. Check `application.properties` file is in `src/main/resources/`
2. Restart the application after configuration changes
3. Check application logs for initialization messages
4. Ensure dependencies are downloaded (run `mvn clean install`)

---

## Testing

1. Restart your application after updating `application.properties`
2. Place an order to test email notifications
3. Check application logs for any errors
4. Verify email is received in the customer's inbox

---

## Note

SMS functionality has been removed from this application. Only email notifications are available.
