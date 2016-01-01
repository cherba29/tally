import ch.qos.logback.classic.Level
import ch.qos.logback.classic.encoder.PatternLayoutEncoder
import ch.qos.logback.classic.filter.LevelFilter
import ch.qos.logback.core.ConsoleAppender
import ch.qos.logback.core.FileAppender
import ch.qos.logback.core.spi.FilterReply

appender("STDOUT", ConsoleAppender) {
  filter(LevelFilter) {
    level = Level.WARN
    onMatch = FilterReply.ACCEPT
    onMismatch = FilterReply.DENY
  }
  encoder(PatternLayoutEncoder) {
    pattern = "%d{HH:mm:ss.SSS} %-5level [%file:%line] - %msg%n"
  }
}
appender("FILE", FileAppender) {
  file = "budget.log"
  append = true
  encoder(PatternLayoutEncoder) {
    pattern = "%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} [%file:%line] - %msg%n"
  }
}

// Set to most detailed level here, and use filter on appenders above.
root(Level.TRACE, ["FILE", "STDOUT"])
