package lila.opening

import chess.format.{ Fen, Forsyth, Uci }
import chess.opening.{ Opening, OpeningDb, OpeningKey, OpeningName, SansStr }
import chess.Replay
import chess.variant.Standard
import chess.{ Situation, Speed }
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat

import lila.common.LilaOpeningFamily

case class OpeningQuery(replay: Replay, config: OpeningConfig):
  val pgn: Vector[String] = replay.state.pgnMoves
  val uci: Vector[Uci]    = replay.moves.view.map(_.fold(_.toUci, _.toUci)).reverse.toVector
  def position            = replay.state.situation
  def variant             = chess.variant.Standard
  val fen                 = Forsyth openingFen replay.state.situation
  val opening             = OpeningDb findByFen fen
  val family              = opening.map(_.family)
  def pgnString           = pgn mkString " "
  def pgnUnderscored      = pgn mkString "_"
  def initial             = pgn.isEmpty
  def query = openingAndExtraMoves match
    case (op, _) => OpeningQuery.Query(op.fold("-")(_.key.value), pgnUnderscored.some)
  def prev = (pgn.sizeIs > 1) ?? OpeningQuery(OpeningQuery.Query("", pgn.init.mkString(" ").some), config)

  val openingAndExtraMoves: (Option[Opening], List[PgnMove]) =
    opening.map(_.some -> Nil) orElse OpeningDb.search(replay).map { case Opening.AtPly(op, ply) =>
      op.some -> pgn.drop(ply + 1).toList
    } getOrElse (none, pgn.toList)

  val name: String = openingAndExtraMoves match
    case (Some(op), Nil)   => op.name.value
    case (Some(op), moves) => s"${op.name}, ${moves mkString " "}"
    case (_, moves)        => moves mkString " "

  override def toString = s"$query $config"

object OpeningQuery:

  case class Query(key: String, moves: Option[String])

  def queryFromUrl(key: String, moves: Option[String]) =
    Query(key, moves.map(_.trim.replace("_", " ")).filter(_.nonEmpty))

  def apply(q: Query, config: OpeningConfig): Option[OpeningQuery] =
    if (q.key.isEmpty && q.moves.isEmpty) fromPgn("", config)
    else q.moves.flatMap(fromPgn(_, config)) orElse byOpening(q.key, config)

  private def byOpening(str: String, config: OpeningConfig) = {
    OpeningDb.shortestLines.get(OpeningKey(str)) orElse
      lila.common.String
        .decodeUriPath(str)
        .map(OpeningName(_))
        .map(OpeningKey.fromName(_))
        .flatMap(OpeningDb.shortestLines.get)
  }.map(_.pgn.value) flatMap { fromPgn(_, config) }

  private def fromPgn(pgn: String, config: OpeningConfig) = for {
    parsed <- chess.format.pgn.Reader.full(pgn).toOption
    replay <- parsed.valid.toOption
  } yield OpeningQuery(replay, config)

  val firstYear  = 2017
  val firstMonth = s"$firstYear-01"
