
Web Server + Google Cloud Storage
==================================

This is a web server running on Google App Engine (project name required).
Web content is read from Google Cloud Storage (bucket name required).
Access is managed by adding users to the GAE project.

## Setup

Download and install Google Cloud SDK, then run these commands to set it up.

```
    gcloud init
    gcloud auth application-default login
    gcloud config set project GOOGLE_CLOUD_PROJECT_NAME
```

Edit src/main/webapp/WEB-INF/appengine-web.xml and modify the value of BUCKET_NAME to a real bucket name. 
The specified bucket hosts the web content.  When updating the website, simply modify files in the bucket.

## Maven
### Running locally

    mvn appengine:run

### Deploying

    mvn appengine:deploy

