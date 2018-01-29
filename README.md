
Web Server + Google Cloud Storage
==================================

This is a web server running on Google App Engine (GAE).
Web content is read from Google Cloud Storage (bucket name required).
Access is managed by adding user to GAE project.

## Setup

Run these commands to set up Google Cloud SDK

```
    gcloud init
    gcloud auth application-default login
    gcloud config set project GOOGLE_CLOUD_PROJECT_NAME
```

Edit src/main/webapp/WEB-INF/appengine-web.xml and modify the value of BUCKET_NAME to a real bucket name. 

## Maven
### Running locally

    mvn appengine:run

### Deploying

    mvn appengine:deploy

