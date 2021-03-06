package cz.payola.web.client.views.graph.table

import scala.collection._
import s2js.adapters.html
import cz.payola.common.rdf._
import cz.payola.web.client.views.elements._
import cz.payola.web.client.views.bootstrap.Icon

class SelectResultPluginView extends TablePluginView("Select Result Table")
{
    private var variables = mutable.ListBuffer.empty[String]

    private var solutions = mutable.HashMap.empty[String, mutable.ListBuffer[Binding]]

    private var bindings = mutable.HashMap.empty[String, Binding]

    private val csvDownloadButton = new Button(new Text("Export to CSV"), "pull-right", new Icon(Icon.list_alt))

    csvDownloadButton.mouseClicked += { e =>
        var csv = ""

        if (variables.nonEmpty) {
            variables.foreach(csv += _ + ",")
            csv = csv.substring(0, csv.length - 1) + "\n"

            solutions.foreach { s =>
                variables.foreach { variable =>
                    csv += s._2.find(_.variable == variable).map(_.value.toString).getOrElse("") + ","
                }
                csv = csv.substring(0, csv.length - 1) + "\n"
            }
        }


        val modal = new CsvExportModal(csv)
        modal.render()
        false
    }

    def fillTable(graph: Option[Graph], tableHead: html.Element, tableBody: html.Element) {
        graph.foreach { g =>
            variables = mutable.ListBuffer.empty[String]
            solutions = mutable.HashMap.empty[String, mutable.ListBuffer[Binding]]
            bindings = mutable.HashMap.empty[String, Binding]

            // Retrieve the bindings.
            g.edges.foreach { e =>
                e.uri match {
                    case u if u.endsWith("#resultVariable") => {
                        variables += e.destination.toString
                    }
                    case u if u.endsWith("#binding") => {
                        val solutionBindings = solutions.getOrElseUpdate(e.origin.uri, mutable.ListBuffer.empty[Binding])
                        val binding = new Binding
                        solutionBindings += binding
                        bindings.put(e.destination.toString, binding)
                    }
                }
            }

            if (variables.isEmpty) {
                tableWrapper.removeAllChildNodes()
                renderMessage(
                    tableWrapper.htmlElement,
                    "The graph isn't a result of a Select SPARQL Query...",
                    "Choose a different visualization plugin."
                )
            } else {
                // Retrieve the binding values.
                g.edges.foreach { e =>
                    e.uri match {
                        case u if u.endsWith("#value") => {
                            bindings(e.origin.toString).value = e.destination
                        }
                        case u if u.endsWith("#variable") => {
                            bindings(e.origin.toString).variable = e.destination.toString
                        }
                    }
                }

                // Create the headers.
                val headerRow = addRow(tableHead)
                variables.foreach { variable =>
                    val cell = addCell(headerRow, isHeader = true)
                    cell.innerHTML = variable
                }

                // Create the body.
                solutions.foreach { s =>
                    val row = addRow(tableBody)
                    variables.foreach { variable =>
                        val cell = addCell(row)
                        s._2.find(_.variable == variable).map(_.value).foreach {
                            case i: IdentifiedVertex => createVertexView(i).render(cell)
                            case v => new Text(v.toString).render(cell)
                        }
                    }
                }
            }
        }
    }

    override def renderControls(toolbar: html.Element) {
        csvDownloadButton.render(toolbar)
    }

    override def destroyControls() {
        csvDownloadButton.destroy()
    }
}

class Binding
{
    var variable: String = ""

    var value: Vertex = null
}
