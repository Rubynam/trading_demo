## Prepare
- Read prompt-template.md

## Task 1
I have  3 endpoints binance
```yaml

    kline-url: https://api.binance.com/api/v3/klines
    ticker: https://api.binance.com/api/v3/ticker/
    depth: https://api.binance.com/api/v3/depth
```
- The first endpoint I already create model and mapping model
- The response of second endpoint
```json
{
"symbol": "BTCUSDT",
"priceChange": "-1930.21000000",
"priceChangePercent": "-2.734",
"weightedAvgPrice": "69619.06468989",
"prevClosePrice": "70589.03000000",
"lastPrice": "68658.81000000",
"lastQty": "0.01474000",
"bidPrice": "68658.80000000",
"bidQty": "0.78597000",
"askPrice": "68658.81000000",
"askQty": "1.80059000",
"openPrice": "70589.02000000",
"highPrice": "71100.94000000",
"lowPrice": "68228.50000000",
"volume": "15609.80328000",
"quoteVolume": "1086739904.34680150",
"openTime": 1774085355011,
"closeTime": 1774171755011,
"firstId": 6134409490,
"lastId": 6136688961,
"count": 2279472
}
```
 - The last endpoint having format below

```json
{"lastUpdateId":90604784256,"bids":[["68717.70000000","3.82236000"],["68717.69000000","0.27343000"],["68717.44000000","0.00250000"],["68717.41000000","0.20901000"],["68717.22000000","0.13324000"],["68717.14000000","0.80222000"],["68717.12000000","0.41610000"],["68717.11000000","0.00315000"],["68716.57000000","0.00008000"],["68715.91000000","0.00839000"]],"asks":[["68717.71000000","0.05200000"],["68717.72000000","0.00080000"],["68717.77000000","0.00375000"],["68717.90000000","0.00016000"],["68717.91000000","0.04016000"],["68718.00000000","0.00080000"],["68718.08000000","0.00009000"],["68718.50000000","0.00016000"],["68718.51000000","0.08016000"],["68718.63000000","0.00008000"]]}
```

- You need to create model, service using restTemplate to raw data.
- The args of function need to symbol typed String, limit typed int and other fields that you need to re-check if needed.

