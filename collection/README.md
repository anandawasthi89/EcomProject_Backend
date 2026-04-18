# Postman Collection

This folder contains import-ready Postman artifacts for testing the backend in the `dev` environment.

For the easiest local demo flow, start the Compose development stack first and then import these files into Postman.

## Files

- `Ecommerce Backend API.postman_collection.json`: full request collection
- `Ecommerce Backend API - Dev.postman_environment.json`: local dev environment variables

## Import Steps

1. Open Postman.
2. Import the collection JSON file.
3. Import the environment JSON file.
4. Select the `Ecommerce Backend API - Dev` environment.
5. Run requests in order:
   - `01 Health`
   - `02 Public User Onboarding`
   - `03 Authentication`
   - `04 Protected Endpoints`
   - `05 Negative Cases`
   - `06 Legacy Alias Endpoints`

## Notes

- The authentication request automatically stores `token`.
- User creation and registration requests automatically store `userId`, `registeredUserId`, and the active user email.
- Protected requests use `Authorization: Bearer {{token}}`.
- Default base URL is `http://localhost:9005`.
- The collection pairs well with Adminer on `http://localhost:8080` when you want to inspect the resulting database rows during a demo.
