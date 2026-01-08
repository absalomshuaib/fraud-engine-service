# Fraud Engine Service Arcitecture


Overall digital banking fraud trends (2022–2024) According to the SABRIC Annual Crime Statistics:

Incident volume

2022: ~36,000 digital banking fraud cases reported.
2023: ~52,500 cases (~45% increase year‑on‑year).
2024: ~97,975 reported digital banking fraud cases (~86% increase from 2023).

Financial losses

2022: R734.7 million in fraud losses.
2023: R1.082 billion (~47% increase).
2024: R1.888 billion (~74% increase).

FNB	Decrease (e.g., 2,197 → 1,452)	suggests fewer complaints or better handling.
MyBroadband
Capitec	Increase (e.g., 1,259 → 1,651)	more complaints, possibly due to growth in customer base or reporting changes.


A large portion of digital fraud is inflicted through methods like:
Phishing (fraudulent links / emails)
Vishing (voice scams)
Smishing (SMS / WhatsApp scams)
Human error — where customers are tricked into authorising payments — continues to be the primary attack vector

So alot of these can also be mitigated by education messages for users, so we constantly remind customers when they login 
on the app on what are the official rails
We improve our user experience on payments flows
integrated with 3D Secure / 3DS2 (like Visa Secure / Mastercard Identity Check) sends a real-time authentication request to your bank.
Your bank pushes a notification to your app, asking you to approve or deny the payment.
non 3ds websites we can monitor and log the transaction history

Linking user on the app:

We need to firstly have a foundation layer where we mark the device as trusted or untrusted based
on certain checks, this allows us to determine that you are who you are and no fraud has been picked up 
on this device yet. 

1. User links on the app with Username
2. By default the user is now Untrusted
    a. Untrusted -> User has limited payment functionality
    b. Trusted -> User has full payment functionality
3. In order to trust the user device
    a. User needs to either trust this device using a previously trusted device through QR code ect
    b. User can make a payment threshold and after certain amount of days when logging in user will be trusted
    c. Trusted location, if you link your device is a location that we already resolved as trusted we can auto trust your device
        e.g latitude + longitute and ssid wifi

These flags help us limit fruad to newly linked devices however we cant restict payments completely

Trusted payment limits e.g:
payments  = 50000
Transfers = 50000

Untrusted payment limits e.g:
payments  = 5000
Transfers = 5000

Payments on flows:

On trusted devices:
User is already trusted, thus use only goes through the fruad engine service, which checks rules on the
behaviour of previous payments

On Untrusted devices:
User is limited to a payment threshould and also is checked by the fruad Engine service

Scenarios for fruad rules  include:
a. Average amount for the last 30 days for a client
b. Count of payments in the last 3 minutes for velocity check
c. Check if a duplicate payment exists (same client, merchant, amount in last 3 min)
d. Get last known country for Geo-Impossible Travel
e. Independant fraud checks should also be done ( can walk you through how this can be achieved)
    e.g Show user duplicate payments screen would you like to proceed
        Confirm screens
        Notify users with a push if we see suspicious behaviour on a newly linked device making payments
        Add report fraud screens when you get a push for approval from online websites
        

We can use a risk score which allows us to determine when and if we should reject the payment
Data also helps our fraud teams for investigations and our BI data teams for analysis 

Note:
This app is just to illustrate the power of kafka events but ideally we would want to use a
Synchronous / REST-first approach

Payment comes in → /initiatePayment endpoint
Payment is validated in same request:
Run all fraud rules
Save PaymentDecision + payment status
return response immediately (APPROVED / REJECTED)

Ideally we would use kafka asynch for logging and mi data purposes to not slow down the payment process after the payment is done and response has been returned

Produce Kafka Events for the below 

+------------+------------+      +-----------+-----------+        +----------------+
| Fraud Engine Consumer   |      | Notification Service  |        | Analytics      |
|  (async, heavy rules)   |      | Push/SMS/email alerts |        | Dashboards /   |
|  - risk scoring         |      | New device, high-risk |        |                |
|  - External checks      |      +---------------------+           +----------------+
|  - Historical replays   |
|  - Audit MI logging     |
+------------------------+

+---------------------+
| PaymentDecision DB  |
| Stores all decisions|
| for audit & replay  |
+---------------------+