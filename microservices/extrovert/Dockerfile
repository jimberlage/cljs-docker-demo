FROM ruby:latest

RUN gem install bundler

# All docker-based apps can pretend they own port 3000.
ENV PORT=3000

EXPOSE 3000

ADD . /opt/extrovert

WORKDIR /opt/extrovert

RUN bundle install

CMD ruby main.rb
