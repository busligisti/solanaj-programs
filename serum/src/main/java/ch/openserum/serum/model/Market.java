package ch.openserum.serum.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.p2p.solanaj.core.PublicKey;

/**
 * Class to represent a Serum market. Should be instantiated using a {@link .MarketBuilder}.
 */
@Getter
@Setter
@ToString(exclude = "eventQueue")
public class Market {

    private AccountFlags accountFlags;
    private PublicKey ownAddress;
    private long vaultSignerNonce;
    private PublicKey baseMint;
    private PublicKey quoteMint;
    private PublicKey baseVault;
    private long baseDepositsTotal;
    private long baseFeesAccrued;
    private PublicKey quoteVault;
    private long quoteDepositsTotal;
    private long quoteFeesAccrued;
    private long quoteDustThreshold;

    private PublicKey requestQueue;
    private PublicKey eventQueueKey;

    private PublicKey bids;
    private PublicKey asks;

    private long baseLotSize;
    private long quoteLotSize;
    private long feeRateBps;
    private long referrerRebatesAccrued;

    private OrderBook bidOrderBook;
    private OrderBook askOrderBook;

    private EventQueue eventQueue;

    // Data from token mints
    private byte baseDecimals;
    private byte quoteDecimals;

    public static Market readMarket(byte[] data) {
        Market market = new Market();

        final AccountFlags accountFlags = AccountFlags.readAccountFlags(data);
        market.setAccountFlags(accountFlags);

        final PublicKey ownAddress = SerumUtils.readOwnAddressPubkey(data);
        market.setOwnAddress(ownAddress);

        final long vaultSignerNonce = SerumUtils.readVaultSignerNonce(data);
        market.setVaultSignerNonce(vaultSignerNonce);

        final PublicKey baseMint = SerumUtils.readBaseMintPubkey(data);
        market.setBaseMint(baseMint);

        final PublicKey quoteMint = SerumUtils.readQuoteMintPubkey(data);
        market.setQuoteMint(quoteMint);

        final PublicKey baseVault = SerumUtils.readBaseVaultPubkey(data);
        market.setBaseVault(baseVault);

        final long baseDepositsTotal = SerumUtils.readBaseDepositsTotal(data);
        market.setBaseDepositsTotal(baseDepositsTotal);

        final long baseFeesAccrued = SerumUtils.readBaseFeesAccrued(data);
        market.setBaseFeesAccrued(baseFeesAccrued);

        final PublicKey quoteVault = SerumUtils.readQuoteVaultOffset(data);
        market.setQuoteVault(quoteVault);

        final long quoteDepositsTotal = SerumUtils.readQuoteDepositsTotal(data);
        market.setQuoteDepositsTotal(quoteDepositsTotal);

        final long quoteFeesAccrued = SerumUtils.readQuoteFeesAccrued(data);
        market.setQuoteFeesAccrued(quoteFeesAccrued);

        final long quoteDustThreshold = SerumUtils.readQuoteDustThreshold(data);
        market.setQuoteDustThreshold(quoteDustThreshold);

        final PublicKey requestQueue = SerumUtils.readRequestQueuePubkey(data);
        market.setRequestQueue(requestQueue);

        final PublicKey eventQueue = SerumUtils.readEventQueuePubkey(data);
        market.setEventQueueKey(eventQueue);

        final PublicKey bids = SerumUtils.readBidsPubkey(data);
        market.setBids(bids);

        final PublicKey asks = SerumUtils.readAsksPubkey(data);
        market.setAsks(asks);

        final long baseLotSize = SerumUtils.readBaseLotSize(data);
        market.setBaseLotSize(baseLotSize);

        final long quoteLotSize = SerumUtils.readQuoteLotSize(data);
        market.setQuoteLotSize(quoteLotSize);

        final long feeRateBps = SerumUtils.readFeeRateBps(data);
        market.setFeeRateBps(feeRateBps);

        final long referrerRebatesAccrued = SerumUtils.readReferrerRebatesAccrued(data);
        market.setReferrerRebatesAccrued(referrerRebatesAccrued);

        return market;
    }

    // TODO - implement all of these, just updating bids, asks, and event queue.
    // However, most of these fields won't change after a reload.
    public void reload(MarketBuilder builder) {
        Market market = builder.reload();
        this.bidOrderBook = market.getBidOrderBook();
        this.askOrderBook = market.getAskOrderBook();
        this.eventQueue = market.getEventQueue();
    }
}
