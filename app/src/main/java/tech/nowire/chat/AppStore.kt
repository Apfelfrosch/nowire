package tech.nowire.chat

import kotlinx.serialization.Serializable

@Serializable
class AppStoreMessage(
    val id: String,
    val text: String,
    val timestamp: Long,
    val senderSignPublicKey: ByteArray,
    val respondingTo: String?,
)

@Serializable
class AppStoreChat(
    val id: Long,
    val send_key: ByteArray,
    val recv_key: ByteArray,
    val otherPublicSignKey: ByteArray,
    val alias: String,
    val selfSignPublicKey: ByteArray,
    val selfSignPrivateKey: ByteArray,
    val messages: List<AppStoreMessage>,
    val amountUnread: Int,
    val pinnedTime: Long,
    val broken: Boolean,
    val timeCreated: Long,
)

@Serializable
class AppStore(
    val chats: List<AppStoreChat>,
    val messagesToSend: List<SendQueueEntry>,
    val deliveryStages: HashMap<String, DeliveryStage>
)
