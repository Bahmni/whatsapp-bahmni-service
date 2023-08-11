# whatsapp-bahmni-service
A microservice for Whatsapp to communicate to Bahmni (for Appointment booking) by Patients.

# Requirements
- java version 1.8
- Apache Maven 3.9.3

# Clone the Repository
> git clone [whatsapp-bahmni-service](https://github.com/Bahmni/whatsapp-bahmni-service.git) repo

> cd whatsapp-bahmni-service

# Build
- Checkout the [whatsapp-bahmni-service](https://github.com/Bahmni/whatsapp-bahmni-service) repository
- Generate the jar file by running `mvn clean install -DskipTests`

# WhatsApp Cloud API Set Up and Webhook Configuration

    Steps Involved in API Set Up:

- A Facebook account to register as a Meta Developer. Follow the steps mentioned here: [link](https://developers.facebook.com/docs/development/register)
- Create a Meta App by following the steps mentioned here: [link](https://developers.facebook.com/docs/development/create-an-app/)
- After creating a Meta App, a Meta Business account is required to set up WhatsApp Cloud API completely.
- After creating Meta Business account, complete the API setup.
- Test the API by sending a message using test number to your WhatsApp.

#
    Steps Involved in Webhook Configuration:

- Webhook Configuration requires a Public Callback URL for the Microservice.
- This HTTPS URL can be created using a tunneling tool called **ngrok** that exposes the local implementation of Microservice to the Internet and provides a public URL.
- Meta configures the webhook using a **Verify Token** provided by the developer.
- Once the webhook is configured notifications about messages sent by the users and delivery status can be received on the callback URL.


> For further help and detailed description of the aforementioned steps, follow the official Meta documentation: [link](https://developers.facebook.com/docs/whatsapp/cloud-api/get-started/)
