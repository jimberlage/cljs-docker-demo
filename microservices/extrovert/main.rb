require 'sinatra'

set(:bind, '0.0.0.0')

port = ENV['PORT']
set(:port, port) if port

MESSAGE = ''

get('/api/message') do
  [200, (MESSAGE.upcase * 8)]
end

post('/api/message') do
  new_message = request['message']
  if new_message
    MESSAGE = new_message

    redirect(to('/'), 303)
  else
    [400, 'You must submit a message with the request.']
  end
end

get('/', provides: 'html') do
  send_file('index.html')
end
