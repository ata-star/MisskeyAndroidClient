package jp.panta.misskeyandroidclient.mfm

class Mention(
    override val start: Int,
    override val end: Int,
    override val insideStart: Int,
    override val insideEnd: Int,
    val text: String,
    val host: String?
) : Element{
    override val elementType: ElementType = ElementType.MENTION
}