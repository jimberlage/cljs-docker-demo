from flask import render_template, request, Flask
app = Flask(__name__)

MESSAGE = 'No message given yet.'

@app.route('/', methods=['GET'])
def root():
    return render_template('index.html', message=MESSAGE)

@app.route('/api/message', methods=['POST'])
def api_message():
    new_message = request.form.get('message')

    if new_message:
        global MESSAGE
        MESSAGE = new_message

        return 'OK'
    else:
        return 'You must submit a message with the request.', 400, {'Content-Type': 'text/html'}

if __name__ == '__main__':
    app.run(host='0.0.0.0')
