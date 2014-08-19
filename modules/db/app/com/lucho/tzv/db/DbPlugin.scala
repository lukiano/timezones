package com.lucho.tzv.db

import play.api.Plugin

trait DbPlugin[T] extends Plugin {
  def db: T
}