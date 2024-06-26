package warped.realms.server.mapper

import adapters.grpc.dao.RequestMessage
import adapters.grpc.dao.ResponseMessage
import adapters.grpc.server.dao.Observer
import dao.Entity
import warped.realms.dao.entitydao.EntityDaoMapper
import dao.GamePackage
import warped.realms.dao.gamepackage.GamePackageMapper
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.ConcurrentMap

class ServerGamePackageController(
    val queue_response: ConcurrentHashMap<Observer, ConcurrentLinkedQueue<ResponseMessage>>,
    val queue_request: ConcurrentHashMap<Observer, ConcurrentLinkedQueue<RequestMessage>>
) {
    private val gamePackageMapper = GamePackageMapper()
    private val entityDaoMapper = EntityDaoMapper()

    private val requests: MutableList<Pair<Observer, MutableList<RequestMessage>>> = mutableListOf()

    private val responses: MutableList<Pair<Observer, MutableList<ResponseMessage>>> = mutableListOf()

//    private val packages: MutableList<GamePackage> = mutableListOf() //cashes of game states
    private val hash_requests: MutableMap<Observer, Int> = mutableMapOf()

    private var hash_size_requests = 0

    fun Update(
        gamePackage: GamePackage
    )
    {
        //Load queues of requests
        loadListeners(queue_request)

        val _requests = mapRequests(requests)

        val entities = _requests.map { (indObserver, request) ->
            val entity = entityDaoMapper.MapEntity(request, indObserver)
            println("[ServerPackageController] {PLAYER: ${indObserver}} request input_x: ${entity.input_x}")
            Entity(indObserver) to entity
        }
        if(entities.size > 0)
            println("[ServerPackageController] entities in game package size: ${entities.size}")
        else
            println("[ServerPackageController] entities in game package size: 0")
        // also need to save it
        //
        //
        gamePackageMapper.MapGamePackage(gamePackage, entities)
    }
    private fun loadListeners
    (
        _queue_request: ConcurrentHashMap<Observer, ConcurrentLinkedQueue<RequestMessage>>
    )
    {
        _queue_request.forEach {
            val (observer, linkedQueue) = it
            getHashRequestsList(observer, linkedQueue)
        }
    }
    private fun getHashRequestsList(
        _observer: Observer,
        _linkedQueue: ConcurrentLinkedQueue<RequestMessage>
    ){
        val index_observer = hash_requests[_observer]

        if(index_observer != null)
        {
            requests[index_observer].second.clear().also {
                requests[index_observer].second.addAll(listFromQueue(_linkedQueue))
            }
            _linkedQueue.clear()
        }
        else
        {
            requests.add(_observer to mutableListOf<RequestMessage>())
            hash_requests[_observer] = requests.lastIndex
            println("[SERVER GAME PACKAGE CONTROLLER] Init new queue for requests INDEX: {${requests.lastIndex}}")

            requests.last().second.clear().also {
                requests.last().second.addAll(listFromQueue(_linkedQueue))
            }
            _linkedQueue.clear()
        }
    }
    private fun getHashResponsesList(
        _observer: Observer
    ): Int {
        val index_observer = hash_requests[_observer]

        if(index_observer != null)
        {
            while(responses.size-1 < index_observer)
            {
                responses.add(_observer to mutableListOf())
                println("[SERVER GAME PACKAGE CONTROLLER]: Added observer with index: {$index_observer}")
            }
        }
        else
        {
            throw Exception("SERVER GAME PACKAGE CONTROLLER: getHashResponsesList()")
        }
        return index_observer
    }
    private fun listFromQueue(
        _linkedQueue: ConcurrentLinkedQueue<RequestMessage>
    ) = mutableListOf<RequestMessage>().apply {
        if(_linkedQueue.size != 0)
        {
            println("POLL REQUEST")
            this.addAll(_linkedQueue)
        }
    }
    fun SendGamePackage(
        gamePackage: GamePackage
    )
    {
        val response = gamePackageMapper.UnmapGamePackage(gamePackage)

        response.forEach { (entity, entityDao) ->
            val ind = getHashResponsesList(requests[entity.index].first)
            responses[ind].second.clear()
            responses[ind].second.add(entityDaoMapper.UnmapEntity(entityDao, response.map { it.second }))
            println("[SERVER GAME PACKAGE CONTROLLER]{PLAYER: ${ind}} responses.size: ${responses[ind].second.size}")
        }
        println("[SERVER GAME PACKAGE CONTROLLER] response queues.size: ${responses.size}")

        if(responses.isNotEmpty())
        {
            queue_response.forEach { (observer, linkedQueue) ->
                val ind = getHashResponsesList(observer)
                linkedQueue.addAll(responses[ind].second)
                println("[SERVER GAME PACKAGE CONTROLLER]{PLAYER: ${ind}} responsesQueue.size: ${linkedQueue.size}")
            }
        }
    }
    private fun mapRequests(
        requests: MutableList<Pair<Observer, MutableList<RequestMessage>>>
    ): MutableList<Pair<Int, RequestMessage>>
    {
        return mutableListOf<Pair<Int, RequestMessage>>().apply {
            hash_requests.forEach { (obs, ind) ->
                if(requests[ind].second.isNotEmpty())
                    this@apply.add(ind to requests[ind].second.last())
            }
        }
    }
}
