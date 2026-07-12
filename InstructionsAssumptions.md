# Trade Execution System

## Running the Application

### Prerequisites
- Java 21
- Maven

### Start the application

```bash
mvn clean spring-boot:run
```

---

## Running Tests

Execute all unit tests:

```bash
mvn test
```

A JaCoCo coverage report will be generated at:

```
target/site/index.html
```

---

# Assumptions & Design Decisions

## Trade Execution

- Trades are executed immediately as **Market Orders** using the latest stored market price.
- The client only provides:
    - `userId`
    - `cryptoPairId`
    - `tradeType`
    - `quantity`
- Price is determined by the server:
    - **BUY** orders execute at the latest **Ask** price.
    - **SELL** orders execute at the latest **Bid** price.

---

## Price Freshness

- Trade execution requires a recent market price.
- If the latest stored market price is more than **30 seconds old**, the trade is rejected to avoid executing using stale market data.
- Users are asked to retry once newer prices become available.

---

## Wallet Behaviour

- BUY orders require sufficient **USDT** balance.
- SELL orders require sufficient balance of the base cryptocurrency (BTC or ETH).
- A cryptocurrency wallet is automatically created when a user purchases that cryptocurrency for the first time.
- Wallets with zero balances are retained and are not automatically deleted.

---

## Supported Trading Pairs

- Only active trading pairs are supported.
- Unsupported or inactive trading pairs are rejected.

---

## Order Execution

- Orders execute immediately once all validation checks pass.
- Partial fills are not supported.
- Orders follow **Fill-or-Kill (FOK)** behaviour:
    - The full quantity must be executable.
    - Otherwise the trade is rejected.
- For the purposes of this assessment, sufficient exchange liquidity is assumed.

---

## Trade Records

- Trade records are immutable once created.
- The implementation only supports inserting new trade records.
- Updating or deleting historical trades is intentionally not implemented.

---

## Scheduler Observation

During testing, I observed that the provided price scheduler:

- swaps **BTCUSDT** and **ETHUSDT** when persisting prices
- swaps **Bid** and **Ask** prices and their corresponding sources

The trade execution service uses the persisted prices as its source of truth and therefore inherits this behaviour.

I did not modify the scheduler implementation because it is outside the scope of the assessment and I could not determine whether this behaviour was intentional.

---

## Additional Notes

- Added `@EnableScheduling` to `TradeApplication` to enable the provided scheduled price updates.
- Trade execution is implemented within a single transactional service method to ensure wallet updates and trade creation succeed or fail together.
- Basic unit tests were added for `TradeServiceImpl` covering successful execution and key validation scenarios.