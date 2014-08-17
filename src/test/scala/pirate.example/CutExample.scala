package pirate.example

import pirate._, Pirate._
import scalaz._, Scalaz._, effect.IO
import java.io.File

sealed trait Cut
case class ByteCut(list: String, split: Boolean, files: File) extends Cut
case class CharCut(list: String, files: File) extends Cut
case class FieldCut(list: String, suppress: Boolean, delimiter: Char, files: File) extends Cut

// FIX support multi arguments, i.e. files
// FIX flag type inference, what even is that?
// FIX sub-type handling
// FIX support explicit read, i.e. 'list'
// FIX descriptions on fields
object CutMain extends PirateMainIO[Cut] {
  val byte: Parse[Cut] = (ByteCut |*| (
    flag[String]('b', "list")
  , switch('n').not
  , arguments.one[File]("file")
  )).map(x => x)

  val char: Parse[Cut] = (CharCut |*| (
    flag[String]('c', "list")
  , arguments.one[File]("file")
  )).map(x => x)

  val field: Parse[Cut] = (FieldCut |*| (
    flag[String]('f', "list")
  , switch('s')
  , flag[Char]('d', "delimiter").default('\t')
  , arguments.one[File]("file")
  )).map(x => x)

  def command: Command[Cut] =
    (byte ||| char ||| field) ~ "cut" ~~
     "This is a demo of the unix cut utlity"

  def run(c: Cut) = c match {
    case ByteCut(list, split, files) =>
      IO.putStrLn(s"""cut -b $list ${if (split) "" else "-n "}$files""")
    case CharCut(list, files) =>
      IO.putStrLn(s"""cut -c $list $files""")
    case FieldCut(list, supress, delimiter, files) =>
      IO.putStrLn(s"""cut -f $list ${if (supress) "" else "-s "}${if (delimiter == '\t') "" else "-d '" + delimiter + "'"}$files""")
  }
}

class CutExample extends test.Spec { def is = s2"""

  Cut Examples
  ============

  cat -b 1 one                             $byte
  cut -b 2 -n two                          $noSplit
  cut -c 3 three                           $char
  cut -f 4 four                            $field
  cut -f 5 -s five                         $supress
  cut -f 6 -d x six                        $delim
  cut -f 7 -d x -s seven                   $delimSupress
  cut -f 7 -s -d x seven                   $supressDelim

  Cut Checks
  ==========

  Name is set                              ${CutMain.command.name == "cut"}
  Description is set                       ${CutMain.command.description == Some("This is a demo of the unix cut utlity")}

"""

  def run(args: String*): ParseError \/ Cut =
    Interpretter.run(CutMain.command.parse, args.toList)

  def byte =
    run("-b", "1", "one") must_==
      ByteCut("1", true, new File("one")).right

  def noSplit =
    run("-b", "2", "-n", "two") must_==
      ByteCut("2", false, new File("two")).right

  def char =
    run("-c", "3", "three") must_==
      CharCut("3", new File("three")).right

  def field =
    run("-f", "4", "four") must_==
      FieldCut("4", false, '\t', new File("four")).right

  def supress =
    run("-f", "5", "-s", "five") must_==
      FieldCut("5", true, '\t', new File("five")).right

  def delim =
    run("-f", "6", "-d", "x", "six") must_==
      FieldCut("6", false, 'x', new File("six")).right

  def delimSupress =
    run("-f", "7", "-d", "x", "-s", "seven") must_==
      FieldCut("7", true, 'x', new File("seven")).right

  def supressDelim =
    run("-f", "8", "-s", "-d", "x", "eight") must_==
      FieldCut("8", true, 'x', new File("eight")).right
}