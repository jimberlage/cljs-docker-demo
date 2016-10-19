FROM ruby:latest

RUN gem install bundler

ENV PORT=3000

EXPOSE 3000

ADD . /opt/extrovert

WORKDIR /opt/extrovert

RUN bundle install

CMD ruby main.rb >> /opt/extrovert/log 2>&1
