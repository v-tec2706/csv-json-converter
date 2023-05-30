package config

import zio.ZIO

trait ConfigProvider {
  def config: ZIO[Any, Throwable, Config]
}

object ConfigProvider {
  def default: ConfigProvider = new ConfigProvider {
    override def config: ZIO[Any, Throwable, Config] = ZIO.succeed(
      Config(
        host = "http://localhost",
        fileStoragePath = "data",
        fileNamePrefix = "result")
    )
  }
}
