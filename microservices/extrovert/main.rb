require 'net/http'
require 'sinatra'
require 'uri'

set(:bind, '0.0.0.0')

port = ENV['PORT']
set(:port, port) if port

class MessageSender
  def initialize
    @queue = []
  end

  def add(message)
    @queue << message
  end

  def run
    Net::HTTP.start(ENV.fetch('INTROVERT_HOST'), ENV.fetch('INTROVERT_PORT')) do |conn|
      loop do
        next_message = @queue.first()

        if next_message
          conn.post('/api/message', URI.encode_www_form({message: next_message}))

          @queue.shift()
        end
      end
    end
  end
end

msg_sender = MessageSender.new()

post('/api/message') do
  new_message = request['message']
  if new_message
    msg_sender.add(new_message)

    redirect(to('/'), 303)
  else
    [400, 'You must submit a message with the request.']
  end
end

get('/', provides: 'html') do
  send_file('index.html')
end

Thread::new() do
  loop do
    begin
      msg_sender.run()
    rescue Errno::ECONNREFUSED
      # Rescue this and try again until the service is up.  Try something a bit more graceful in production, this is
      # just here for the demo.
      sleep(2)
    end
  end
end
