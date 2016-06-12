import ssl
import urllib2

url = 'https://localhost:4440'

context = ssl.SSLContext(ssl.PROTOCOL_SSLv23)
context.load_cert_chain('../certs/certs/client.crt', keyfile='../certs/certs/client.key', password='client-password')
context.load_verify_locations('../certs/certs/rootCA.pem')
context.verify_mode = ssl.CERT_REQUIRED

response = urllib2.urlopen(url, context=context)

print ''
print 'Response:'
print vars(response)
print ''
print 'Headers:'
print response.headers.headers
print ''
print 'Content:'
print response.read()
print ''
