package adapters.rest.model.dto

import adapters.rest.model.exception.AlreadyExistException
import adapters.rest.model.exception.InternalServerException
import adapters.rest.model.exception.NotFoundException
import adapters.rest.model.exception.UnauthorizedException
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(value = InternalServerException::class, name = "exc1"),
    JsonSubTypes.Type(value = NotFoundException::class, name = "exc2"),
    JsonSubTypes.Type(value = UnauthorizedException::class, name = "exc3"),
    JsonSubTypes.Type(value = AlreadyExistException::class, name = "exc4"),
    JsonSubTypes.Type(value = TokenResponse::class, name = "token"),
)
interface Response
