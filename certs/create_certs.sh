# References:
# http://datacenteroverlords.com/2012/03/01/creating-your-own-ssl-certificate-authority/
# http://support.nordicedge.com/nsd1309-creating-self-signed-certificates-using-openssl/

mkdir -p certs
rm certs/*

echo Creating root key...
openssl genrsa -des3 -out certs/rootCA.key -passout pass:root-password 2048

echo Self signing root key...
openssl req -x509 -new -nodes -key certs/rootCA.key -days 1024 -out certs/rootCA.pem -passin pass:root-password -subj "/C=US/ST=New York/L=New York/O=Personal/OU=Root/CN=Root Cert"



echo Creating server cert...
openssl genrsa -des3 -out certs/server.key -passout pass:server-password 2048

echo Creating signing request for server cert...
openssl req -new -key certs/server.key -out certs/server.csr -passin pass:server-password -subj "/C=US/ST=New York/L=New York/O=Personal/OU=Server/CN=localhost"

echo Signing server cert...
openssl x509 -req -in certs/server.csr -CA certs/rootCA.pem -CAkey certs/rootCA.key -CAcreateserial -out certs/server.crt -days 500 -sha256 -passin pass:root-password

echo Creating a server key with no password:
openssl rsa -in certs/server.key -out certs/server.no-password.key -passin pass:server-password



echo Creating client cert...
openssl genrsa -des3 -out certs/client.key -passout pass:client-password 2048

echo Creating signing request for client cert...
openssl req -new -key certs/client.key -out certs/client.csr -passin pass:client-password -subj "/C=US/ST=New York/L=New York/O=Personal/OU=Client/CN=Client Cert"

echo Signing client cert...
openssl x509 -req -in certs/client.csr -CA certs/rootCA.pem -CAkey certs/rootCA.key -CAcreateserial -out certs/client.crt -days 500 -sha256 -passin pass:root-password

echo Creating client pkcs12...
openssl pkcs12 -export -in certs/client.crt -inkey certs/client.key -out certs/browser.p12 -passin pass:client-password -passout pass:browser-password -name "Browser Cert"
