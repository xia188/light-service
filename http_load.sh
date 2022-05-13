hostPort=localhost:8083
clients=3
seconds=5

urls=/tmp/urls
rm -f $urls
echo "http://$hostPort/hybrid/id/card?idNumber=511623" >> $urls
echo "http://$hostPort/hybrid/ip/region?ip=121.12.21.23" >> $urls
echo "http://$hostPort/hybrid/bank/card?bankCardNumber=451811549678" >> $urls
http_load -parallel $clients -seconds $seconds $urls
