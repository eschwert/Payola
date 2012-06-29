package cz.payola.data.entities.plugins.parameters

import cz.payola.data.entities.plugins.Parameter
import cz.payola.data.PayolaDB

/**
  * This object converts [[cz.payola.common.entities.plugins.parameters.StringParameter]]
  * to [[cz.payola.data.entities.plugins.parameters.StringParameter]]
  */
object StringParameter
{
    def apply(p: cz.payola.common.entities.plugins.parameters.StringParameter): StringParameter = {
        p match {
            case param: StringParameter => param
            case _ => new StringParameter(p.id, p.name, p.defaultValue)
        }
    }
}

class StringParameter(
    override val id: String,
    name: String,
    defaultVal: String)
    extends cz.payola.domain.entities.plugins.parameters.StringParameter(name, defaultVal)
    with Parameter[String]
{
    private lazy val _valuesQuery = PayolaDB.valuesOfStringParameters.left(this)

    // Get, store and set default value of parameter to Database
    val _defaultValueDb = defaultVal

    override def defaultValue = _defaultValueDb

    def parameterValues: Seq[StringParameterValue] = evaluateCollection(_valuesQuery)

    /**
      * Associates specified [[cz.payola.data.entities.plugins.parameters.StringParameter]].
      *
      * @param p - [[cz.payola.data.entities.plugins.parameters.StringParameter]] to associate
      */
    def associateParameterValue(p: StringParameterValue) {
        associate(p, _valuesQuery)
    }
}

