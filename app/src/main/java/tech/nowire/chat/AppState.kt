package tech.nowire.chat

import kotlinx.serialization.json.Json
import tech.nowire.chat.hcl.LibSodiumUtils
import tech.nowire.chat.hcl.Ratchet
import java.io.File
import java.util.LinkedList
import java.util.Queue
import java.util.UUID
import kotlin.jvm.optionals.getOrNull

class Chat(
    val id: Long,
    val send: Ratchet,
    val recv: Ratchet,
    val otherSignPublicKey: ByteArray,
    val alias: String,
    val selfSignPublicKey: ByteArray,
    val selfSignPrivateKey: ByteArray,
    val messages: MutableList<Message>,
    var amountUnread: Int,
    var pinnedTime: Long,
    var broken: Boolean,
    val timeCreated: Long,
) {

    fun isPinned(): Boolean = pinnedTime > 0

}

enum class DeliveryStage {
    NOT_SENT,
    SENT,
}

class Message(
    val id: UUID,
    val text: String,
    val timestamp: Long,
    val senderSignPublicKey: ByteArray,
    val respondingTo: UUID?,
)

class AppState(
    val chats: MutableList<Chat>,
    val sendQueue: Queue<SendQueueEntry>,
    val key: ByteArray,
    val storeFile: File,
    val deliveryStages: HashMap<String, DeliveryStage>
) {

    companion object {
        fun fromEncryptedJson(key: ByteArray, storeFile: File, json: ByteArray): AppState? {
            val s = LibSodiumUtils.stringSymmetricDecrypt(key, json).getOrNull() ?: return null
            val decoded = Json.decodeFromString<AppStore>(s)
            return AppState(
                decoded.chats.map {
                    Chat(
                        it.id,
                        Ratchet(it.send_key),
                        Ratchet(it.recv_key),
                        it.otherPublicSignKey,
                        it.alias,
                        it.selfSignPublicKey,
                        it.selfSignPrivateKey,
                        it.messages.map {
                            Message(
                                UUID.fromString(it.id),
                                it.text,
                                it.timestamp,
                                it.senderSignPublicKey,
                                it.respondingTo?.let { UUID.fromString(it) }
                            )
                        }.toMutableList(),
                        it.amountUnread,
                        it.pinnedTime,
                        it.broken,
                        it.timeCreated,
                    )
                }.toMutableList(),
                LinkedList(decoded.messagesToSend),
                key,
                storeFile,
                decoded.deliveryStages,
            )
        }
    }

    @Synchronized
    fun toEncryptedJson(): ByteArray? {
        val toStore = AppStore(chats.map {
            AppStoreChat(
                it.id,
                it.send.currentKey,
                it.recv.currentKey,
                it.otherSignPublicKey,
                it.alias,
                it.selfSignPublicKey,
                it.selfSignPrivateKey,
                it.messages.map {
                    AppStoreMessage(
                        it.id.toString(),
                        it.text,
                        it.timestamp,
                        it.senderSignPublicKey,
                        it.respondingTo?.toString()
                    )
                },
                it.amountUnread,
                it.pinnedTime,
                it.broken,
                it.timeCreated,
            )
        }, sendQueue.toList(), deliveryStages)

        val s = Json.encodeToString(toStore)
        return LibSodiumUtils.stringSymmetricEncrypt(key, s).getOrNull()
    }

    @Synchronized
    fun saveAsEncryptedJson() {
        storeFile.parentFile?.let {
            if (!it.exists()) {
                it.mkdirs();
            }
        }

        if (!storeFile.exists()) {
            storeFile.createNewFile()
        }

        storeFile.writeBytes(toEncryptedJson()!!)
    }

}

object PinnedComparator : Comparator<Chat> {
    override fun compare(o1: Chat, o2: Chat): Int {
        return when {
            o1.pinnedTime < o2.pinnedTime -> -1
            o1.pinnedTime > o2.pinnedTime -> 1
            else -> 0
        }
    }
}

val FullChatComparator: java.util.Comparator<Chat> = (PinnedComparator.thenComparing { chat: Chat ->
    return@thenComparing chat.messages.lastOrNull()?.timestamp ?: chat.timeCreated
}.thenComparing { chat: Chat ->
    return@thenComparing chat.alias
}).reversed()
