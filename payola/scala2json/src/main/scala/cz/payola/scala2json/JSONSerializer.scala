package cz.payola.scala2json

object JSONSerializerOptions {
    /** Default options are condensed printing.
     *
     * Condensed printing removes all unnecessary white space, which results in
     * smaller data, but is not well human-readable.
     *
     * Pretty printing option adds tabs to make the output readable, which can
     * be used for debugging.
     *
     * No other options available currently.
     */

    val JSONSerializerOptionCondensedPrinting = 0 << 0
    val JSONSerializerOptionPrettyPrinting = 1 << 0
    val JSONSerializerOptionIgnoreNullValues = 1 << 1

    val JSONSerializerDefaultOptions = (JSONSerializerOptionCondensedPrinting |
                                                                 JSONSerializerOptionIgnoreNullValues)
}

import annotations.JSONFieldName
import JSONSerializerOptions._
import cz.payola.scala2json.annotations.JSONTransient

import scala.collection.mutable.StringBuilder
import java.lang.reflect.Field
import collection.mutable.ArrayBuffer

// TODO: Traits
// TODO: decide what to do with generic maps.

class JSONSerializer(val obj: Any, val options: Int = JSONSerializerDefaultOptions, 
                            val processedObjects: ArrayBuffer[Any] = new ArrayBuffer[Any]()) {

    val prettyPrint: Boolean = (options & JSONSerializerOptionPrettyPrinting) != 0
    val ignoreNullValues: Boolean = (options & JSONSerializerOptionIgnoreNullValues) != 0
    
    if (obj != null){
        if (processedObjects.contains(obj))
            throw new JSONSerializationException("Cycle detected on object " + obj + ".")
        else
            // Do not detect cycles on primitive types
            if (obj.isInstanceOf[AnyRef]
            && !obj.isInstanceOf[String]
            && !obj.isInstanceOf[java.lang.Number]
            && !obj.isInstanceOf[java.lang.Boolean]
            && !obj.isInstanceOf[java.lang.Character])
                processedObjects += obj
    }


    /** Appends a field to string builder.
      *
      * @param f The field
      * @param builder The builder
      * @param isFirst Whether the field is first - if it is, the comma separator is left out.
      *
      * @return False if the field has been skipped.
      */
    private def _appendFieldToStringBuilder(f: Field, builder: StringBuilder, isFirst: Boolean): Boolean = {
        f.setAccessible(true)

        if (_isFieldTransient(f)){
            false
        }else{
            val fieldName = _nameOfField(f)
            val fieldValue = f.get(obj.asInstanceOf[AnyRef])
            if (fieldValue == null && ignoreNullValues){
                false
            }else{
                _appendKeyValueToStringBuilder(fieldName, fieldValue, builder, isFirst)
                true
            }
        }
    }
    
    private def _appendKeyValueToStringBuilder(key: String, value: Any,  builder: StringBuilder, isFirst: Boolean) = {
        if (!isFirst){
            builder.append(',')
            if (prettyPrint){
                builder.append('\n')
            }
        }

        if (prettyPrint){
            builder.append('\t')
        }

        builder.append(key)
        builder.append(':')
        if (prettyPrint){
            builder.append(' ')
        }

        val serializer: JSONSerializer =  new JSONSerializer(value, options, processedObjects.clone.clone)
        val serializedObj: String = serializer.stringValue
        if (prettyPrint)
            builder.append(serializedObj.replaceAllLiterally("\n", "\n\t"))
        else
            builder.append(serializedObj)
    }
    
    
    /** Returns whether @f has a JSONTransient annotation.
     *
     * @param f The field.
     *
     * @return True or false.
     */
    private def _isFieldTransient(f: Field): Boolean = {
        f.getAnnotation(classOf[JSONTransient]) != null
    }

    /** Returns the field's name - if it has a JSONFieldName annotation,
     *  it uses that.
     *
     *  @param f The field.
     *
     *  @return The field's name, considering annotations.
     */
    private def _nameOfField(f: Field): String = {
        val nameAnot = f.getAnnotation(classOf[JSONFieldName])
        if (nameAnot == null)
            f.getName
        else
            if (_validateFieldName(nameAnot.name))
                nameAnot.name
            else
                throw new JSONSerializationException("Name annotation isn't valid for '" + nameAnot.name + "'")
    }

    /** Serializes an Array[_]
     *
     * @param options See stringValue
     *
     * @return JSON representation of obj.
     */
    private def _serializeArray: String = {
        val builder: StringBuilder = new StringBuilder("[")
        if (prettyPrint)
            builder.append('\n')
        
        // We know it is an Array[_]
        val arr: Array[_] = obj.asInstanceOf[Array[_]]

        for (i: Int <- 0 until arr.length) {
            if (i != 0){
                builder.append(',')
                if (prettyPrint)
                    builder.append('\n')
            }

            if (prettyPrint)
                builder.append('\t')
            
            val serializer: JSONSerializer =  new JSONSerializer(arr(i), options, processedObjects.clone)
            val serializedObj: String = serializer.stringValue
            if (prettyPrint)
                builder.append(serializedObj.replaceAllLiterally("\n", "\n\t"))
            else
                builder.append(serializedObj)
        }

        if (prettyPrint)
            builder.append('\n')
        
        builder.append(']')
        builder.toString
    }

    /** Serializes an "array" - i.e. an object that implements
     *  the Iterable trait, yet isn't a map.
     *
     * @param options See stringValue
     *
     * @return JSON representation of obj.
     */
    private def _serializeIterable: String = {
        val builder: StringBuilder = new StringBuilder("[")
        if (prettyPrint)
            builder.append('\n')
        
        // We know it is Iterable
        val coll: Iterable[_] = obj.asInstanceOf[Iterable[_]]

        // Need to keep track of index so that
        // we don't add a comma after the first iteration
        var index: Int = 0
        coll foreach { item => {
            if (index != 0){
                builder.append(',')
                if (prettyPrint)
                    builder.append('\n')
            }
            index += 1

            if (prettyPrint)
                builder.append('\t')

            val serializer: JSONSerializer =  new JSONSerializer(item, options, processedObjects.clone)
            val serializedObj: String = serializer.stringValue
            if (prettyPrint)
                builder.append(serializedObj.replaceAllLiterally("\n", "\n\t"))
            else
                builder.append(serializedObj)
        }}

        if (prettyPrint)
            builder.append('\n')
            
        builder.append(']')
        builder.toString
    }
    

    /** Serializes an object that implements
     *  the Map trait, yet isn't a map.
     *
     * @param options See stringValue
     *
     * @return JSON representation of obj.
     */
    private def _serializeMap: String = {
        val builder: StringBuilder = new StringBuilder("{")
        if (prettyPrint)
            builder.append('\n')
        
        // We know it is a Map[String, _]
        val map: Iterable[(String, _)] = obj.asInstanceOf[Iterable[(String, _)]]

        // Need to keep track of index so that
        // we don't add a comma after the first iteration
        var index: Int = 0
        map foreach {case (key, value) => {
            if (!_validateFieldName(key))
                throw new JSONSerializationException("Cannot use key named '" + key + "' in a map (" + map + ")")
            _appendKeyValueToStringBuilder(key, value, builder, index == 0)
            index += 1
        }}

        if (prettyPrint)
            builder.append('\n')
        
        builder.append('}')
        builder.toString
    }

    /** Matches the @obj's type and calls the appropriate method.
     *
     * @param options See stringValue
     *
     * @return JSON representation of obj.
     */
    private def _serializeObject: String = {
        // *** Map is Iterable as well, but we shouldn't make it an array of
        // one-member dictionaries, rather make it a dictionary as a whole.
        // This is why Map **needs** to be matched first before Iterable.
        obj match {
            case _: String => JSONUtilities.escapedString(obj.asInstanceOf[String])
            case _: java.lang.Number => obj.toString
            case _: java.lang.Boolean => if (obj.asInstanceOf[java.lang.Boolean].booleanValue) "true"
                                         else "false"
            case _: java.lang.Character => JSONUtilities.escapedChar(obj.asInstanceOf[java.lang.Character].charValue())
            case _: scala.collection.immutable.Map[String, _] => _serializeMap
            case _: scala.collection.mutable.Map[String, _] => _serializeMap
            case _: Map[_,_] => throw new JSONSerializationException("Cannot serialize another maps than [String, _] - " + obj)
            case _: Iterable[_] => _serializeIterable
            case _: Array[_] => _serializeArray
            case _: AnyRef => _serializePlainObject
            case _ => _serializePrimitiveType
        }
    }

    /** Serializes an object - generally AnyRef 
      *
      * For most types, just calls obj.toString, the exception is
      * Boolean, which is converted to 'true' or 'false', Char is converted
      * to String. When Unit is encountred, an exception is raised.
      *
      * @return JSON value.
      */
    private def _serializePlainObject: String = {
        // Now we're dealing with some kind of an object,
        // we'll use Java's reflection to serialize it
        val builder: StringBuilder = new StringBuilder("{")

        if (prettyPrint)
            builder.append('\n')
            
        // Get object's fields:
        val c: Class[_] = obj.getClass
        val fields: Array[Field] = c.getDeclaredFields

        var haveProcessedField: Boolean = false
        for (i: Int <- 0 until fields.length) {
            if (_appendFieldToStringBuilder(fields(i), builder, i == 0)){
                haveProcessedField = true
            }
        }

        if (prettyPrint)
            builder.append('\n')
        
        builder.append('}')
        builder.toString
    }

    /** Serializes a primitive type.
      *
      * For most types, just calls obj.toString, the exception is
      * Boolean, which is converted to 'true' or 'false', Char is converted
      * to String. When Unit is encountred, an exception is raised.
      *
      * @return JSON value.
      */
    private def _serializePrimitiveType: String = {
        obj match {
            case _: Boolean => if (obj.asInstanceOf[Boolean]) "true" else "false"
            case _: Char => JSONUtilities.escapedChar(obj.asInstanceOf[Char])
            case _: Unit => throw new JSONSerializationException("Cannot serialize Unit.")
            case _ => obj.toString
        }
    }

    /** The field name in JSON mustn't have spaces, etc. - just as a variable name
      * in scala or C.
      *
      *  @param name Field name.
      *
      *  @return True or false.
      */
    private def _validateFieldName(name: String): Boolean = {
        name.matches("[a-zA-Z0-9_]+")
    }

    /** Serializes @obj to a JSON string.
     *
     * @return JSON representation of obj.
     */
   def stringValue: String = {
       // If obj is null, return "null" - as defined at http://www.json.org/
       // We can't simply ignore it, even though the options would have us
       // ignore null values - this needs to be eliminated earlier in the chain
       if (obj == null){
           "null"
       }else{

           // We need to distinguish several cases:
           // a) obj is a collection -> create an array
           // b) obj is a hash map or similar -> create a dictionary
           // c) obj is a string -> just escape it
           // d) obj is a scala.lang.Array -> create an array
           // e) obj is a primitive type -> convert it
           // f) obj is a regular object -> use reflection to create a dictionary
           //
           // Also have in mind that we support a special value handling
           // if the object implements some of the abstract traits in package
           // cz.payola.scala2json.traits
           _serializeObject
       }
   }

}
