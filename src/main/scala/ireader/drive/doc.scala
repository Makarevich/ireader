package ireader.drive

import java.util.Date

case class BaseDocRecord(
    base: Int,          // base priority
    half: Int,          // priority halflife in hours
    ts: Long            // last read timestamp (in seconds since 01/01/1970)
) {
    def inflate(now: Long) = DocRecord.inflate(this, now)
}

case class DocRecord(
    current: Float,     // current priority
    base: Int,          // base priority
    half: Int,          // priority halflife in hours
    ts: Date,           // last read timestamp
    deadline: Date      // time when current goes below 1.0
)

object DocRecord {
    def inflate(info: BaseDocRecord, now: Long): DocRecord = {
        inflate(info.base, info.half, info.ts, now)
    }

    def inflate(base: Int, half: Int, ts: Long, now: Long): DocRecord = {
        val real_half = half * 3600
        val current = Math.pow(0.5, (now - ts).toFloat / real_half) * base.toFloat
        val deadline = real_half * Math.log(base) / Math.log(2) + ts
        DocRecord(current.toFloat,
                  base,
                  half,
                  new Date(ts*1000),
                  new Date(deadline.toLong*1000))
    }
}

