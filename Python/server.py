# References:
# https://www.piware.de/2011/01/creating-an-https-server-in-python/

import BaseHTTPServer
import SimpleHTTPServer
import ssl

httpd = BaseHTTPServer.HTTPServer(('localhost', 4440), SimpleHTTPServer.SimpleHTTPRequestHandler)
httpd.socket = ssl.wrap_socket(
    httpd.socket,
    keyfile='../certs/certs/server.no-password.key',
    certfile='../certs/certs/server.crt',
    server_side=True,
    cert_reqs=ssl.CERT_REQUIRED,
    ca_certs='../certs/certs/rootCA.pem'
)

sa = httpd.socket.getsockname()
print 'Serving HTTPS on', sa[0], 'port', sa[1], '...'

httpd.serve_forever()
