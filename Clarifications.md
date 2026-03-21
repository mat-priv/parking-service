1. What is template for registration number? (I will assume the registration number will be like "AB12A45" where "AB" is a combination of 2 letters and "12A34" is a combination of 5 digits/letters)
2. What is total amount of parking slots? should be configurable? (I will assume that total amount of parking slots is 100 and it should be configurable)
3. How we calculate parking time? If car was parked for 2 min and 13 sec, should we charge for 2 or 3 minutes? What if car was parked for 2 min 45 sec?
4. What is billingID? Is it UUID or something else?
5. Does parking has only one entry? Should the code be concurrent? (I will assume that parking has 2+ entries and code should be concurrent)
6. Assume that the extra charge amount and time is configurable