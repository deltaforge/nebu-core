# Nebu Core

## What is it?
This project is the main middleware layer in the Nebu software ecosystem. This
middleware offers a RESTful API outlined
[here](http://api-portal.anypoint.mulesoft.com/nebu/api/nebu). In addition is
communicates with the Virtual Machine Manager extensions trough their API (see
the VMM VMware project for more information). 

## How do you run it?

### Dependancies and Configuration
This project depends on [Nebu Common
Java](https://github.com/deltaforge/nebu-common-java), please read the
instructions there on how to install the library. Secondly you need to prepare a
config file, which is the config.xml file in the root of the project. A sample
file has been provided and requires you to only fill the fields. For Nebu Core
you need to specify server.port to be the port you want Nebu Core to listen on.
In addition you need to specify the IP and port that the VMM extension is
currently running on.

### Running the program
Since this project uses maven and a simple run script has been provided, you can
simply execute `./run` in the root of the project once the setup is done.
