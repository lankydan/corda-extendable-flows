Corda 4 was released last week (21st Feb?????) bringing with it a ton of new features to make Corda more enjoyable to work with. To be honest, I am kind of assuming there are a lot of new features. I had a quick browse through the changelog, mainly to see my contributions being referenced, but I remember seeing a lot of lines of text. That has to be a good thing right?

Anyway, one of these features is the ability to extend and override Flows. It doesn't really sound very fancy when you realise that Corda is written in Kotlin and has inheritance completely baked into it (true for Java as well). But, there is more to it than that. Corda needs to map an Initiating Flow to the counterparty Flow that is responding to it. 

This is fine when two parties are using the same CorDapp. There is no extra complexity added in this situation. If, on the other hand, the counterparty wanted to send some data to an external system upon receiving a transaction, how could they do that? The original CorDapp does not know or care about this system and therefore it cannot cater to these needs. Being able to solve this sort of problem allows developers to build upon existing CorDapps and adapt them to be more suitable their use-case. Furthermore, once good practices are layed out, extending third-party CorDapps will become easier and remove the need for teams of developers to constantly reinvent the wheel when others have already solved part of a problem. Obviously, this assumes that there is access to these external CorDapps, but it is completely in the realm of possibility. Especially with the [R3 Marketplace](https://marketplace.r3.com/solutions?referrer=featured-solutions) already showcasing a collection.

In this post we will be focusing on extending and overriding Flows. Furthermore, we will take the perspective of two different viewpoints. 
- The developer/maintainer of a CorDapp
- A developer wanting to use and adapt an existing CorDapp

For the process to work, both sides must put in the effort to write their applications in appropriate manner so that the benefits can be leveraged.

We will start by looking at what the original CorDapp must contain and then what a developer must do to extend it.

Before we go any further, here is a link to the [official documentation on extending and overriding flows](https://docs.corda.net/head/flow-overriding.html#configuring-responder-flows).

### Writing a base Flow to allow extension

Writing a CorDapp in a way that allows it to be easily extended will likely require a reasonable amount of thought. It largely depends on what a CorDapp maintainer is trying to achieve. Providing a way for developers to extend a CorDapp so that they can send data to external systems or add their own logging should pose no problems. On the other hand, allowing the contents of a transaction to be altered or who it is sent to will require more thought to ensure that a CorDapp is not misused. This is a subject I hope to explore a bit further in future posts.

For the purpose of this post, we will look at the simpler option. Let's jump right in since there has a been a whole lot of text so far and no code. Below is the `SendMessageFlow` that will act as the "base" Flow that will be extended in a later section:
```kotlin
@InitiatingFlow
@StartableByRPC
open class SendMessageFlow(private val message: MessageState) :
  FlowLogic<SignedTransaction>() {

  open fun preTransactionBuild() {
    // to be implemented by sub type flows - otherwise do nothing
  }

  open fun preSignaturesCollected(transaction: SignedTransaction) {
    // to be implemented by sub type flows - otherwise do nothing
  }

  open fun postSignaturesCollected(transaction: SignedTransaction) {
    // to be implemented by sub type flows - otherwise do nothing
  }

  open fun postTransactionCommitted(transaction: SignedTransaction) {
    // to be implemented by sub type flows - otherwise do nothing
  }

  @Suspendable
  final override fun call(): SignedTransaction {
    logger.info("Started sending message ${message.contents}")
    preTransactionBuild()
    val tx = verifyAndSign(transaction())
    preSignaturesCollected(tx)
    val sessions = listOf(initiateFlow(message.recipient))
    val stx = collectSignature(tx, sessions)
    postSignaturesCollected(stx)
    return subFlow(FinalityFlow(stx, sessions)).also {
      logger.info("Finished sending message ${message.contents}")
      postTransactionCommitted(it)
    }
  }

  // collectSignature

  // verifyAndSign

  // transaction
}
```
I have removed a few of the functions to so we can focus on what is important.

The first and sometimes important step to allow this class to be extended, is the fact it is `open`. This is more of a Kotlin thing rather than Java, since all classes in Kotlin are `final` by default. If you are writing this in Java then just ignore the last few sentences!

Following on from that, there are a series of functions that are available to be overridden. Each function has been placed in an appropriate place inside the main execution of the Flow. They will then be called when the Flow runs. For now, they have been given empty implementations since they provide no use to the CorDapp developer.

I regards to the `open` functions. You can name them or place them wherever you want. These are functions that I thought could be useful for developers wanting to add extra traceability over what the base app provides. 

Digging down into a bit more detail. The `call` function has been made `final` (the same as in Java) to prevent the whole contents of the Flow from being overridden. If anyone wants to take your Flow and completely replace its "main" functionality, then what is the point? To me it seems kind of dodgy. To remove that possibility making it `final` is a smart move.

Later on we will look at how this Flow can be subclassed.

Below is the `SendMessageResponder` that interacts with the `SendMessageFlow`. It follows the same concepts as above and therefore I will only show it as a reference for later:
```kotlin
@InitiatedBy(SendMessageFlow::class)
open class SendMessageResponder(private val session: FlowSession) : FlowLogic<Unit>() {

  open fun postTransactionSigned(transaction: SignedTransaction) {
    // to be implemented by sub type flows - otherwise do nothing
  }

  open fun postTransactionCommitted(transaction: SignedTransaction) {
    // to be implemented by sub type flows - otherwise do nothing
  }

  @Suspendable
  final override fun call() {
    val stx = subFlow(object : SignTransactionFlow(session) {
      override fun checkTransaction(stx: SignedTransaction) {}
    })
    postTransactionSigned(stx)
    val committed = subFlow(
      ReceiveFinalityFlow(
        otherSideSession = session,
        expectedTxId = stx.id
      )
    )
    postTransactionCommitted(committed)
  }
}
```

### Extending an existing Initiating Flow

In this section we get to see how the developer can make use of the work done on the previous Flow. It already has all the needed functionality. The only thing that is missing is the small amount of extra traceability that the developer wants to add in. Thanks to the functions added to the base Flow. This should cause no problems.

Let's start with extending an Initiating Flow. The requirements for doing so are as follows:
- Extend the base `@InitiatingFlow`
- Do __not__ add `@InitiatingFlow` to the new Flow (errors will occur if you do)
- Reference the base Flow's constructor (`super` in Java)
- Override any desired functions
- Call the new Flow instead of the base Flow

After reading that list, you might have realised that this is pretty much a description of inheritance in Object Oriented languages (like Kotlin and Java). There might be more going on inside Corda to allow this to work but from your perspective, you are just writing normal Object Oriented code like usual.

Taking these requirements we can see what an extended Flow might look like:
```kotlin
@StartableByRPC
class CassandraSendMessageFlow(private val message: MessageState) :
  SendMessageFlow(message) {

  override fun preTransactionBuild() {
    serviceHub.cordaService(MessageRepository::class.java).save(
      message,
      sender = true,
      committed = false
    )
    logger.info("Starting transaction for message: $message")
  }

  override fun preSignaturesCollected(transaction: SignedTransaction) {
    val keys = transaction.requiredSigningKeys - ourIdentity.owningKey
    logger.info("Collecting signatures from $keys for transaction for message: $message")
  }

  override fun postSignaturesCollected(transaction: SignedTransaction) {
    logger.info("Collected signatures for transaction for message: $message")
  }

  override fun postTransactionCommitted(transaction: SignedTransaction) {
    serviceHub.cordaService(MessageRepository::class.java).save(
      message,
      sender = true,
      committed = true
    )
    logger.info("Committed transaction for message: $message")
  }
}
```
I have left in all the noisy functions that implement the extra traceability I was talking about, but that is due to how empty the class would be without them. Since `call` does not need to be implemented. This Flow only needs to override the `open` functions. To be honest, it doesn't _need_ to override them at all, they are optional. If desired, this Flow could override a single function and then be left empty.

Have all the requirements listed above been met?
- `CassandraSendMessageFlow` extends `SendMessageFlow`
- There is no `@InitiatingFlow` in sight
- In Kotlin you must call the `super` constructor anyway, so that's done
- In this scenario, all the functions have overridden
- We haven't got this far

Ok, so that is 4/5 so far. That's a pretty good start. To cross off the last item on the list we need to see how it's called. Below are snippets that call the base `SendMessageFlow` and the `CassandraSendMessageFlow` extending Flow.

Starting with `SendMessageFlow`:
```kotlin
proxy.startFlow(::SendMessageFlow, messageState)
```
Followed by `CassandraSendMessageFlow`:
```kotlin
proxy.startFlow(::CassandraSendMessageFlow, messageState)
```
Notice the difference? In this scenario, only the name of the Flow has changed. Nothing else.

Both snippets are completely valid. Calling the original `SendMessageFlow` is still allowed. Remember from our perspective, it is just normal Object Oriented code. It won't have the fancy extra code added to the extending Flow but it will still execute without issues. Completing this step meets the last requirement for extending an `@InitiatingFlow`.

Before we end this section, here is an important piece of information to remember:
> You must ensure the sequence of sends/receives/subFlows in a subclass are compatible with the parent.

I will put this into all of the following sections since failing to follow this will cause your Flows fail.

### Extending a Responder Flow

Extending a Responder Flow works in a very similar way to extending an `@InitiatingFlow` Flow. The only difference being how it is called. As stated in the [documentation](https://docs.corda.net/head/flow-overriding.html#subclassing-a-flow):
> Corda would detect that both `BaseResponder` and `SubResponder` are configured for responding to Initiator. Corda will then calculate the hops to `FlowLogic` and select the implementation which is furthest distance, ie: the most subclassed implementation.

The statement, "most subclassed" is the important takeaway from this text. Therefore from a developer's viewpoint, all they need to do is extend the external base Responder Flow and thats it. I quite liked previous the requirements list, so let's go through another one for extending Responder Flows:
- Extend the base `@InitiatedBy` / Responder Flow
- Add `@InitiatedBy` to the new Flow
- Reference the base Flow's constructor (`super` in Java)
- Override any desired functions

If you are vigilant, you might have noticed that there is no mention of how to call it. The extending Responder Flow does not need to be called or referenced anywhere else. Corda will do the work to route everything to the right location.

Just to be sure, let's have a quick look at an example:
```kotlin
@InitiatedBy(SendMessageFlow::class)
class CassandraSendMessageResponder(session: FlowSession) :
  SendMessageResponder(session) {

  override fun postTransactionSigned(transaction: SignedTransaction) {
    val message = transaction.coreTransaction.outputsOfType<MessageState>().single()
    logger.info("Signed transaction for message: $message")
  }

  override fun postTransactionCommitted(transaction: SignedTransaction) {
    val message = transaction.coreTransaction.outputsOfType<MessageState>().single()
    serviceHub.cordaService(MessageRepository::class.java).save(
      message,
      sender = false,
      committed = true
    )
    logger.info("Committed transaction for message: $message")
  }
}
```
Furthermore, lets look back at the statement "most subclassed" again. The `CassandraSendMessageResponder` is a subclass of `SendMessageResponder` and is therefore chosen by Corda to handle requests from the Initiating Flow. But, this could be taken a step further. If there was another class, say `SuperSpecialCassandraSendMessageResponder`, this Flow is now what Corda will start using. Although I do find this sort of scenario somewhat unlikely at the moment, it is definitely worth knowing about.

Copying and pasting this statement again so you don't forget:
> You must ensure the sequence of sends/receives/subFlows in a subclass are compatible with the parent.

### Overriding a Responder Flow

This is purposely a separate section. Here we will talk specifically about overriding a Responder Flow rather than extending one. Why would you do this and what is the difference? Answering the first question, a developer may want to write a Responder Flow that greatly diverges from the original base Flow but still needs to interact with the specific Initiating Flow provided by an external CorDapp. To achieve this they can override the Flow. Another word to describe this could be "replace". The original base Flow is completely replaced by the overriding Flow. There is no involvement of extension in this situation.

I think the [Corda documentation's](https://docs.corda.net/head/flow-overriding.html#overriding-a-flow-via-node-configuration) wording on this subject is quite good:
> Whilst the subclassing approach is likely to be useful for most applications, there is another mechanism to override this behaviour. This would be useful if for example, a specific CordApp user requires such a different responder that subclassing an existing flow would not be a good solution.

Hopefully, this extract along with my earlier description will clarify the difference between extending and overriding Responder Flows.

So, what might an overriding Flow look like? Well anything you want really, within reason. Maybe it might look like the below, although I doubt it:
```kotlin
@InitiatedBy(SendMessageFlow::class)
class OverridingResponder(private val session: FlowSession) :
  FlowLogic<Unit>() {

  @Suspendable
  override fun call() {
    val stx = subFlow(object : SignTransactionFlow(session) {
      override fun checkTransaction(stx: SignedTransaction) {}
    })
    logger.info("Screw the original responder. I'll build my own responder... with blackjack and hookers!")
    subFlow(
      ReceiveFinalityFlow(
        otherSideSession = session,
        expectedTxId = stx.id
      )
    )
  }
}
```
Since this Flow is completely replacing the original base Flow, it will look just like a normal Responder Flow. Since, well, it is one. That means it has `@InitiatedBy` referencing the Initiating Flow, extends `FlowLogic` and implements the `call` function.

Just putting this here one last time:
> You must ensure the sequence of sends/receives/subFlows in a subclass are compatible with the parent.

This is even more prevalent here than in the previous sections. Since the whole `call` function is being overridden you must make sure that every `send` and `receive` is in the right place so interactions with the Initiating Flow run without errors.

Configuration wise there is a bit more to do than with extending a Flow. In this situation we are trying to completely replace a Responder with another. To do so, we need a way to tell the node to redirect interactions from an Initiating Flow to a new overriding Responder Flow. Corda provides a way to do just that. 

To specify the redirection add the following to your `node.conf`:
```conf
flowOverrides {
    overrides=[
        {
            initiator="com.lankydanblog.tutorial.base.flows.SendMessageFlow"
            responder="com.lankydanblog.tutorial.cassandra.flows.OverridingResponder"
        }
    ]
}
```
Obviously, change the classes referenced to your own...

So what is going on here? The config says that the `SendMessageFlow` which normally interacts with `SendMessageResponder` will now route to `OverridingResponder` instead.

To make everything a bit easier as well, the `Cordform` plugin provides the `flowOverride` method as part of `deployNodes`. This will then generate the configuration block above for you. For the example above, the following code was used:
```groovy
node {
  name "O=PartyA,L=London,C=GB"
  p2pPort 10002
  rpcSettings {
    address("localhost:10006")
    adminAddress("localhost:10046")
  }
  rpcUsers = [[user: "user1", "password": "test", "permissions": ["ALL"]]]
  cordapp(project(':cordapp-contracts-states'))
  cordapp(project(':cordapp'))
  cordapp(project(':cordapp-extended-cassandra'))
  // the important part
  flowOverride("com.lankydanblog.tutorial.base.flows.SendMessageFlow",
    "com.lankydanblog.tutorial.cassandra.flows.OverridingResponder")
}
```
Now, after `deployNodes` has run and you have started your node, any requests coming from `SendMessageFlow` or any of its subclasses will now route communication to the `OverridingResponder`.

## Conclusion

One of the handy features that Corda 4 provides is the ability to customise Flows from third-party CorDapps (or your own). This is done by two methods, extending or overriding. 

Extending would be my first choice between the two but it does require a bit more effort on the side of the CorDapp developer. They must provide enough avenues for customisation without relinquishing control of the original functionality of their Flows. Providing to little customisation might not deter other developers from using their CorDapp. But, developers could become unhappy with the lack of control of their own application. It is a slippery slope to control original intent with routes for customisation. On the other hand, actually extending a Flow does not require much work, making it easier for developers to adopt and adapt external Flows.

Overriding on the other hand requires no work for a CorDapp developer and instead everything is put onto the developer leveraging external Responder Flows. That is because the the existing Flow is pretty much being thrown away and the only reference back to the original implementation is the link to the Initiating Flow.

By embracing both the extending and overriding of Flows, CorDapp developers will be able to leverage external CorDapps while still providing enough customisation to fulfil all business requirements they might have. As time progresses, developers will drive the adoption of reusing existing CorDapps as they provide access to additional customisation, soon taking the same position as Open Source libraries that we all already leverage in any work we do.

The code used in this post can be found on my [GitHub](https://github.com/lankydan/corda-extendable-flows). It contains the code for `CassandraSendMessageFlow` which sets up a connection to an external Cassandra database to save tracing style data. It also contains another module that sends HTTP requests as part of its extension of the base Flows. If you are still curious after reading this post, this repository might help.

If you enjoyed this post or found it helpful (or both) then please feel free to follow me on Twitter at [@LankyDanDev](https://twitter.com/LankyDanDev) and remember to share with anyone else who might find this useful!