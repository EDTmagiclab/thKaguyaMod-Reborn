package thKaguyaMod.entity.spellcard;

/** スペルカードを定義するインターフェース（1.20.1 移植版） */
public interface ITHSpellCard
{
	/** スペルカードの個別の処理を行う */
	public void spellcard_main();

	/** スペルカードを宣言するときに必要なレベルを返す */
	public int getSpellCardLevel();
}
