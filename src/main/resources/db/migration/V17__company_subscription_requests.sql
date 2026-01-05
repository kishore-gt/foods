-- Add meal type to packages (breakfast/lunch/dinner)
ALTER TABLE subscription_packages ADD COLUMN meal_type VARCHAR(30) NOT NULL DEFAULT 'LUNCH';

-- Add request/approval/payment metadata to company subscriptions
ALTER TABLE company_subscriptions ADD COLUMN approval_note VARCHAR(255);
ALTER TABLE company_subscriptions ADD COLUMN people_count INT;
ALTER TABLE company_subscriptions ADD COLUMN duration_months INT DEFAULT 1;
ALTER TABLE company_subscriptions ADD COLUMN total_amount DECIMAL(10,2);
ALTER TABLE company_subscriptions ADD COLUMN preferred_time VARCHAR(50);
ALTER TABLE company_subscriptions ADD COLUMN excluded_days VARCHAR(255);
ALTER TABLE company_subscriptions ADD COLUMN payment_status VARCHAR(30) DEFAULT 'PENDING' NOT NULL;

-- Allow request lifecycle by relaxing NOT NULL on dates
ALTER TABLE company_subscriptions MODIFY start_date DATE NULL;
ALTER TABLE company_subscriptions MODIFY end_date DATE NULL;

-- Default status for new rows handled in application; existing rows untouched.
