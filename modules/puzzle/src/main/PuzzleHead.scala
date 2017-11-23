package lila.puzzle

case class PuzzleHead(
    _id: lila.user.User.ID,
    current: Option[PuzzleId],
    last: PuzzleId
) {

  def id = _id
}

object PuzzleHead {

  object BSONFields {
    val id = "_id"
    val current = "current"
    val last = "last"
  }

  import reactivemongo.bson._

  private[puzzle] implicit val puzzleHeadBSONHandler = Macros.handler[PuzzleHead]
}
