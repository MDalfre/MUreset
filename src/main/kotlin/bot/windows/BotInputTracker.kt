package io.github.mdalfre.bot.windows

object BotInputTracker {
    @Volatile
    private var lastBotInputMs: Long = 0L

    fun markBotInput() {
        lastBotInputMs = System.currentTimeMillis()
    }

    fun isRecent(windowMs: Long): Boolean {
        val elapsed = System.currentTimeMillis() - lastBotInputMs
        return elapsed in 0..windowMs
    }
}
