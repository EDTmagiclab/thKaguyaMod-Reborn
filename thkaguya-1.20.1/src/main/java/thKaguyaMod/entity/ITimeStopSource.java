package thKaguyaMod.entity;

/**
 * 「時間停止の発生源」を表す共通インタフェース（1.20.1 移植版で新規追加）。
 *
 * <p>{@link thKaguyaMod.entity.item.EntitySakuyaWatch} と
 * {@link thKaguyaMod.entity.item.EntitySakuyaStopWatch} の両方が実装する。</p>
 *
 * <p>もともと原版は「クライアント側に懐中時計が居るか」だけで判定していたが、
 * それだと「全速か半速か」「誰を凍らせないか」「半径はいくつか」がクライアントに伝わらない。
 * そこで必要な情報を同期データ（{@code SynchedEntityData}）に載せ、このインタフェース越しに読む。
 * クライアントの {@link TimeStopManager} ミラーは、この 5 つの値だけでサーバと同じ判定を再現する。</p>
 */
public interface ITimeStopSource
{
    /** この発生源が実際に時間操作を駆動しているか（コンフィグで無効なら false） */
    boolean isTimeStopActive();

    /** {@code EntitySakuyaWatch.TIME_STOP} 等のモード値 */
    int getTimeStopMode();

    /** 術者の entity id。不明なら -1 */
    int getTimeStopOwnerId();

    /** 停止空間の最大半径（サーバのコンフィグ値が同期されてくる） */
    int getTimeStopRadius();

    /** true = 半径内の敵味方問わず凍らせる（コンフィグ {@code timeStopHostileOnly=false}） */
    boolean isTimeStopFreezeAll();
}
