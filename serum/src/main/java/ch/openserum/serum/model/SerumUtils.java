package ch.openserum.serum.model;

import org.bitcoinj.core.Utils;
import org.p2p.solanaj.core.PublicKey;
import org.p2p.solanaj.rpc.RpcClient;
import org.p2p.solanaj.rpc.RpcException;
import org.p2p.solanaj.rpc.types.Memcmp;
import org.p2p.solanaj.rpc.types.ProgramAccount;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * version 2 market offsets.
 *
 *   blob(5), 0-4
 *   accountFlagsLayout('accountFlags'), 5-12
 *   publicKeyLayout('ownAddress'), 13-44
 *   u64('vaultSignerNonce'), 45-52
 *   publicKeyLayout('baseMint'), 53-84
 *   publicKeyLayout('quoteMint'), 85-116
 *   publicKeyLayout('baseVault'), 117-148
 *   u64('baseDepositsTotal'), 149-156
 *   u64('baseFeesAccrued'), 157-164
 *   publicKeyLayout('quoteVault'), 165-196
 *
 *   u64('quoteDepositsTotal'), 197-204
 *   u64('quoteFeesAccrued'), 205-212
 *
 *   u64('quoteDustThreshold'), 213-220
 *
 *   publicKeyLayout('requestQueue'), 221-252
 *   publicKeyLayout('eventQueue'), 253-284
 *
 *   publicKeyLayout('bids'), 285-316
 *   publicKeyLayout('asks'), 317-348
 *
 *
 *   u64('baseLotSize'), 349-356
 *   u64('quoteLotSize'), 357-364
 *
 *   u64('feeRateBps'), 365-372
 *
 *   u64('referrerRebatesAccrued') 373-380
 *
 *   blob(7);
 *
 *   ....
 *
 */

/**
 * Utility class for reading/building Serum-related objects
 */
public class SerumUtils {

    private static final Logger LOGGER = Logger.getLogger(SerumUtils.class.getName());

    private static final String PADDING = "serum";

    // Types
    public static final int U8_SIZE_BYTES = 1;
    public static final int INT32_SIZE_BYTES = 4;
    public static final int U64_SIZE_BYTES = 8;
    public static final int U128_SIZE_BYTES = 16;

    // Market
    public static final long LAMPORTS_PER_SOL = 1000000000L;
    private static final int ACCOUNT_FLAGS_SIZE_BYTES = 8;
    private static final int ACCOUNT_FLAGS_OFFSET = 5;
    public static final int OWN_ADDRESS_OFFSET = ACCOUNT_FLAGS_OFFSET + ACCOUNT_FLAGS_SIZE_BYTES;
    private static final int VAULT_SIGNER_NONCE_OFFSET = OWN_ADDRESS_OFFSET + PublicKey.PUBLIC_KEY_LENGTH;
    private static final int BASE_MINT_OFFSET = VAULT_SIGNER_NONCE_OFFSET + U64_SIZE_BYTES;
    private static final int QUOTE_MINT_OFFSET = BASE_MINT_OFFSET + PublicKey.PUBLIC_KEY_LENGTH;
    private static final int BASE_VAULT_OFFSET = QUOTE_MINT_OFFSET + PublicKey.PUBLIC_KEY_LENGTH;
    private static final int BASE_DEPOSITS_TOTAL_OFFSET = BASE_VAULT_OFFSET + PublicKey.PUBLIC_KEY_LENGTH;
    private static final int BASE_FEES_ACCRUED_OFFSET = BASE_DEPOSITS_TOTAL_OFFSET + U64_SIZE_BYTES;
    private static final int QUOTE_VAULT_OFFSET = BASE_FEES_ACCRUED_OFFSET + U64_SIZE_BYTES;
    private static final int QUOTE_DEPOSITS_TOTAL_OFFSET = QUOTE_VAULT_OFFSET + PublicKey.PUBLIC_KEY_LENGTH;
    private static final int QUOTE_FEES_ACCRUED_OFFSET = QUOTE_DEPOSITS_TOTAL_OFFSET + U64_SIZE_BYTES;
    private static final int QUOTE_DUST_THRESHOLD_OFFSET = QUOTE_FEES_ACCRUED_OFFSET + U64_SIZE_BYTES;
    private static final int REQUEST_QUEUE_OFFSET = QUOTE_DUST_THRESHOLD_OFFSET + U64_SIZE_BYTES;
    private static final int EVENT_QUEUE_OFFSET = REQUEST_QUEUE_OFFSET + PublicKey.PUBLIC_KEY_LENGTH;
    private static final int BIDS_OFFSET = EVENT_QUEUE_OFFSET + PublicKey.PUBLIC_KEY_LENGTH;
    private static final int ASKS_OFFSET = BIDS_OFFSET + PublicKey.PUBLIC_KEY_LENGTH;
    private static final int BASE_LOT_SIZE_OFFSET = ASKS_OFFSET + PublicKey.PUBLIC_KEY_LENGTH;
    private static final int QUOTE_LOT_SIZE_OFFSET = BASE_LOT_SIZE_OFFSET + U64_SIZE_BYTES;
    private static final int FEE_RATE_BPS_OFFSET = QUOTE_LOT_SIZE_OFFSET + U64_SIZE_BYTES;
    private static final int REFERRER_REBATES_ACCRUED_OFFSET = FEE_RATE_BPS_OFFSET + U64_SIZE_BYTES;

    // New Order
    private static final int NEW_ORDER_STRUCT_LAYOUT = 10;
    private static final int NEW_ORDER_INDEX = 1;
    private static final int SIDE_LAYOUT_INDEX = NEW_ORDER_INDEX + INT32_SIZE_BYTES;
    private static final int LIMIT_PRICE_INDEX = SIDE_LAYOUT_INDEX + INT32_SIZE_BYTES;
    private static final int MAX_BASE_QUANTITY_INDEX = LIMIT_PRICE_INDEX + U64_SIZE_BYTES;
    private static final int MAX_QUOTE_QUANTITY_INDEX = MAX_BASE_QUANTITY_INDEX + U64_SIZE_BYTES;
    private static final int SELF_TRADE_BEHAVIOR_INDEX = MAX_QUOTE_QUANTITY_INDEX + U64_SIZE_BYTES;
    private static final int ORDER_TYPE_INDEX = SELF_TRADE_BEHAVIOR_INDEX + INT32_SIZE_BYTES;
    private static final int CLIENT_ID_INDEX = ORDER_TYPE_INDEX + INT32_SIZE_BYTES;
    private static final int LIMIT_INDEX = CLIENT_ID_INDEX + U64_SIZE_BYTES;


    // Token mint
    private static final int TOKEN_MINT_DECIMALS_OFFSET = 44;

    // Open orders account
    private static final int MARKET_FILTER_OFFSET = 13;
    private static final int OWNER_FILTER_OFFSET = MARKET_FILTER_OFFSET + PublicKey.PUBLIC_KEY_LENGTH;

    public static final PublicKey SERUM_PROGRAM_ID_V3 = new PublicKey("9xQeWvG816bUx9EPjHmaT23yvVM2ZWbrrpZb9PusVFin");
    public static final PublicKey WRAPPED_SOL_MINT = new PublicKey("So11111111111111111111111111111111111111112");

    public static PublicKey readOwnAddressPubkey(byte[] bytes) {
        return PublicKey.readPubkey(bytes, OWN_ADDRESS_OFFSET);
    }

    public static long readVaultSignerNonce(byte[] bytes) {
        return Utils.readInt64(bytes, VAULT_SIGNER_NONCE_OFFSET);
    }

    public static PublicKey readBaseMintPubkey(byte[] bytes) {
        return PublicKey.readPubkey(bytes, BASE_MINT_OFFSET);
    }

    public static PublicKey readQuoteMintPubkey(byte[] bytes) {
        return PublicKey.readPubkey(bytes, QUOTE_MINT_OFFSET);
    }

    public static PublicKey readBaseVaultPubkey(byte[] bytes) {
        return PublicKey.readPubkey(bytes, BASE_VAULT_OFFSET);
    }

    public static long readBaseDepositsTotal(byte[] bytes) {
        return Utils.readInt64(bytes, BASE_DEPOSITS_TOTAL_OFFSET);
    }

    public static long readBaseFeesAccrued(byte[] bytes) {
        return Utils.readInt64(bytes, BASE_FEES_ACCRUED_OFFSET);
    }

    public static PublicKey readQuoteVaultOffset(byte[] bytes) {
        return PublicKey.readPubkey(bytes, QUOTE_VAULT_OFFSET);
    }

    public static long readQuoteDepositsTotal(byte[] bytes) {
        return Utils.readInt64(bytes, QUOTE_DEPOSITS_TOTAL_OFFSET);
    }

    public static long readQuoteFeesAccrued(byte[] bytes) {
        return Utils.readInt64(bytes, QUOTE_FEES_ACCRUED_OFFSET);
    }

    public static long readQuoteDustThreshold(byte[] bytes) {
        return Utils.readInt64(bytes, QUOTE_DUST_THRESHOLD_OFFSET);
    }

    public static PublicKey readRequestQueuePubkey(byte[] bytes) {
        return PublicKey.readPubkey(bytes, REQUEST_QUEUE_OFFSET);
    }

    public static PublicKey readEventQueuePubkey(byte[] bytes) {
        return PublicKey.readPubkey(bytes, EVENT_QUEUE_OFFSET);
    }

    public static PublicKey readBidsPubkey(byte[] bytes) {
        return PublicKey.readPubkey(bytes, BIDS_OFFSET);
    }

    public static PublicKey readAsksPubkey(byte[] bytes) {
        return PublicKey.readPubkey(bytes, ASKS_OFFSET);
    }

    public static long readBaseLotSize(byte[] bytes) {
        return Utils.readInt64(bytes, BASE_LOT_SIZE_OFFSET);
    }

    public static long readQuoteLotSize(byte[] bytes) {
        return Utils.readInt64(bytes, QUOTE_LOT_SIZE_OFFSET);
    }

    public static long readFeeRateBps(byte[] bytes) {
        return Utils.readInt64(bytes, FEE_RATE_BPS_OFFSET);
    }

    public static long readReferrerRebatesAccrued(byte[] bytes) {
        return Utils.readInt64(bytes, REFERRER_REBATES_ACCRUED_OFFSET);
    }

    public static void writeNewOrderStructLayout(ByteBuffer result) {
        result.put(NEW_ORDER_INDEX, (byte) NEW_ORDER_STRUCT_LAYOUT);
    }

    public static void writeSideLayout(ByteBuffer result, SideLayout sideLayout) {
        result.put(SIDE_LAYOUT_INDEX, (byte) sideLayout.getValue());
    }

    public static void writeLimitPrice(ByteBuffer result, long price) {
        result.putLong(LIMIT_PRICE_INDEX, price);
    }

    public static void writeMaxBaseQuantity(ByteBuffer result, long maxBaseQuantity) {
        // 9 + 8 bytes for the 64bit limit price = index 17 (or 16 indexed maybe)
        // lets verify with some real requests
        // looks good, used a quantity of 1, showing as 1
        result.putLong(MAX_BASE_QUANTITY_INDEX, maxBaseQuantity);
    }

    public static void writeMaxQuoteQuantity(ByteBuffer result, long maxQuoteQuantity) {
        // TODO - figure out what this is, for now just write it
        result.putLong(MAX_QUOTE_QUANTITY_INDEX, maxQuoteQuantity);
    }


    // Only need to write the first byte since the enum is small
    public static void writeSelfTradeBehavior(ByteBuffer result, SelfTradeBehaviorLayout selfTradeBehavior) {
        result.put(SELF_TRADE_BEHAVIOR_INDEX, (byte) selfTradeBehavior.getValue());
    }

    public static void writeOrderType(ByteBuffer result, OrderTypeLayout orderTypeLayout) {
        result.put(ORDER_TYPE_INDEX, (byte) orderTypeLayout.getValue());
    }

    public static void writeClientId(ByteBuffer result, long clientId) {
        result.putLong(CLIENT_ID_INDEX, clientId);
    }

    public static void writeLimit(ByteBuffer result) {
        result.putShort(LIMIT_INDEX, (short) 65535);
    }

    /**
     * Reads the decimals value from decoded account data of a given token mint
     *
     * Note: MINT_LAYOUT = struct([blob(44), u8('decimals'), blob(37)]);
     *
     * 0-43 = other data
     * index 44 = the single byte of decimals we want
     * 45-... = other data
     *
     * @param accountData decoded account data from the token mint
     * @return int containing the number of decimals in the token mint
     */
    public static byte readDecimalsFromTokenMintData(byte[] accountData) {
        // Read a SINGLE byte at offset 44
        byte result = accountData[TOKEN_MINT_DECIMALS_OFFSET];
        //LOGGER.info(String.format("Market decimals byte = %d", result));

        return result;
    }

    public static void validateSerumData(byte[] accountData) {
        for (int i = 0; i < 5; i++) {
            if (accountData[i] != PADDING.getBytes()[i]) {
                throw new RuntimeException("Invalid Event Queue data.");
            }
        }
    }

    public static double getBaseSplTokenMultiplier(byte baseDecimals) {
        return Math.pow(10, baseDecimals);
    }

    public static double getQuoteSplTokenMultiplier(byte quoteDecimals) {
        return Math.pow(10, quoteDecimals);
    }

    public static float priceLotsToNumber(long price, byte baseDecimals, byte quoteDecimals, long baseLotSize, long quoteLotSize) {
        double top = (price * quoteLotSize * getBaseSplTokenMultiplier(baseDecimals));
        double bottom = (baseLotSize * getQuoteSplTokenMultiplier(quoteDecimals));

        return (float) (top / bottom);
    }

    public static long priceNumberToLots(float price, Market market) {
        return priceNumberToLots(
                price,
                market.getQuoteDecimals(),
                market.getBaseLotSize(),
                market.getBaseDecimals(),
                market.getQuoteLotSize()
        );
    }

    public static long priceNumberToLots(float price, byte quoteDecimals, long baseLotSize, byte baseDecimals, long quoteLotSize) {
        double top = (price * Math.pow(10, quoteDecimals) * baseLotSize);
        double bottom = Math.pow(10, baseDecimals) * quoteLotSize;
        return Math.round(top / bottom);
    }

    public static float baseSizeLotsToNumber(long size, long baseLotSize, long baseMultiplier) {
        double top = size * baseLotSize;
        return (float) (top / baseMultiplier);
    }

    public static long baseSizeNumberToLots(float size, byte baseDecimals, long baseLotSize) {
        double top = Math.round(size * Math.pow(10, baseDecimals));
        return (long) (top / baseLotSize);
    }

    public static OpenOrdersAccount findOpenOrdersAccountForOwner(RpcClient client, PublicKey marketAddress, PublicKey ownerAddress) {
        int dataSize = 3228;

        List<ProgramAccount> programAccounts = null;

        Memcmp marketFilter = new Memcmp(MARKET_FILTER_OFFSET, marketAddress.toBase58());
        Memcmp ownerFilter = new Memcmp(OWNER_FILTER_OFFSET, ownerAddress.toBase58());

        List<Memcmp> memcmpList = List.of(marketFilter, ownerFilter);

        try {
            programAccounts = client.getApi().getProgramAccounts(SERUM_PROGRAM_ID_V3, memcmpList, dataSize);
        } catch (RpcException e) {
            e.printStackTrace();
        }

        OpenOrdersAccount openOrdersAccount = null;

        if (programAccounts != null) {
            Optional<ProgramAccount> optionalAccount = programAccounts.stream()
                    .findFirst();
            if (optionalAccount.isPresent()) {
                ProgramAccount openOrdersProgramAccount = optionalAccount.get();
                byte[] data = openOrdersProgramAccount.getAccount().getDecodedData();
                openOrdersAccount = OpenOrdersAccount.readOpenOrdersAccount(data);
                openOrdersAccount.setOwnPubkey(PublicKey.valueOf(openOrdersProgramAccount.getPubkey()));
            }
        }

        return openOrdersAccount;
    }

    public static long getLamportsNeededForSolWrapping(float price, float size, boolean isBuy, OpenOrdersAccount openOrdersAccount) {
        long lamports;

        if (isBuy) {
            lamports = Math.round(price * size * 1.01 * LAMPORTS_PER_SOL);
            if (null != openOrdersAccount) {
                lamports -= openOrdersAccount.getQuoteTokenFree();
            }
        } else {
            lamports = (long) (size * LAMPORTS_PER_SOL);
            if (null != openOrdersAccount) {
                lamports -= openOrdersAccount.getBaseTokenFree();
            }
        }

        return Math.max(lamports, 0) + 10000000;
    }

    public static long getMaxQuoteQuantity(float price, float size, Market market) {
        return market.getQuoteLotSize() *
                baseSizeNumberToLots(size, market.getBaseDecimals(), market.getBaseLotSize()) *
                priceNumberToLots(price, market);
    }

    public static PublicKey getVaultSigner(Market market){
        final ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(market.getVaultSignerNonce());
        byte[] vaultSignerNonce = buffer.array();

        final PublicKey vaultSigner = PublicKey.createProgramAddress(
                List.of(
                        market.getOwnAddress().toByteArray(),
                        vaultSignerNonce
                ),
                SerumUtils.SERUM_PROGRAM_ID_V3
        );

        return vaultSigner;
    }
}