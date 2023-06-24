from http.server import BaseHTTPRequestHandler, HTTPServer

count = 0


class SimpleHTTPRequestHandler(BaseHTTPRequestHandler):
    def do_POST(self):
        global count
        count += 1
        print(f"Count= {count}")
        self.send_response(200)
        self.send_header('Content-type', 'text/plain')
        self.end_headers()
        self.wfile.write(b'Success')


def run(server_class=HTTPServer, handler_class=SimpleHTTPRequestHandler, port=8000):
    server_address = ('', port)
    httpd = server_class(server_address, handler_class)
    print(f"Server running on port {port}")
    httpd.serve_forever()


run()
