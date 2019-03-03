<!-- wp:paragraph -->
<p>Corda 4 was released last week (21st Feb) bringing with it a ton of new features to make Corda more enjoyable to work with. To be honest, I am kind of assuming there are a lot of new features. I had a quick browse through the changelog, mainly to see my contributions being referenced, but I remember seeing a lot of lines of text. That has to be a good thing right?</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Anyway, one of these features is the ability to extend and override Flows. It doesn't really sound very fancy when you realise that Corda is written in Kotlin and has inheritance completely baked into it (true for Java as well). But, there is more to it than that. Corda needs to map an Initiating Flow to the counterparty Flow that is responding to it.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>This is fine when two parties are using the same CorDapp. There is no extra complexity added in this situation. If, on the other hand, the counterparty wanted to send some data to an external system upon receiving a transaction, how could they do that? The original CorDapp does not know or care about this system and therefore it cannot cater to these needs. Being able to solve this sort of problem allows developers to build upon existing CorDapps and adapt them to be more suitable for their use-case. Furthermore, once good practices are laid out, extending third-party CorDapps will become easier and remove the need for teams of developers to constantly reinvent the wheel when others have already solved part of a problem. Obviously, this assumes that there is access to these external CorDapps, but it is completely in the realm of possibility. Especially with the <a href="https://marketplace.r3.com/solutions?referrer=featured-solutions" target="_blank" rel="noreferrer noopener" aria-label="R3 Marketplace (opens in a new tab)">R3 Marketplace</a> already showcasing a collection.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>In this post, we will be focusing on extending and overriding Flows. Furthermore, we will take the perspective of two different viewpoints. </p>
<!-- /wp:paragraph -->

<!-- wp:list -->
<ul><li>The developer/maintainer of a CorDapp</li><li>A developer wanting to use and adapt an existing CorDapp</li></ul>
<!-- /wp:list -->

<!-- wp:paragraph -->
<p>For the process to work, both sides must put in the effort to write their applications in an appropriate manner so that the benefits can be leveraged.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>We will start by looking at what the original CorDapp must contain and then what a developer must do to extend it.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Before we go any further, here is a link to the <a href="https://docs.corda.net/head/flow-overriding.html#configuring-responder-flows" target="_blank" rel="noreferrer noopener" aria-label="official documentation on extending and overriding flows (opens in a new tab)">official documentation on extending and overriding flows</a>.</p>
<!-- /wp:paragraph -->

<!-- wp:heading {"level":3} -->
<h3>Writing a base Flow to allow extension</h3>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>Writing a CorDapp in a way that allows it to be easily extended will likely require a reasonable amount of thought. It largely depends on what a CorDapp maintainer is trying to achieve. Providing a way for developers to extend a CorDapp so that they can send data to external systems or add their own logging should pose no problems. On the other hand, allowing the contents of a transaction to be altered or who it is sent to will require more thought to ensure that a CorDapp is not misused. This is a subject I hope to explore a bit further in future posts.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>For the purpose of this post, we will look at the simpler option. Let's jump right in since there has a been a whole lot of text so far and no code. Below is the <code>SendMessageFlow</code> that will act as the "base" Flow that will be extended in a later section:</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>[gist https://gist.github.com/lankydan/021a54afc4438eaffae0e2997b0e2154 /]</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>I have removed a few of the functions so we can focus on what is important.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>The first and sometimes important step to allow this class to be extended is the fact it is <code>open</code>. This is more of a Kotlin thing rather than Java since all classes in Kotlin are <code>final</code> by default. If you are writing this in Java then just ignore the last few sentences!</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Following on from that, there are a series of functions that are available to be overridden. Each function has been placed in an appropriate place inside the main execution of the Flow. They will then be called when the Flow runs. For now, they have been given empty implementations since they provide no use to the CorDapp developer.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>In regards to the <code>open</code> functions. You can name them or place them wherever you want. These are functions that I thought could be useful for developers wanting to add extra traceability over what the base app provides.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Digging down into a bit more detail. The <code>call</code> function has been made <code>final</code> (the same as in Java) to prevent the whole contents of the Flow from being overridden. If anyone wants to take your Flow and completely replace its "main" functionality, then what is the point? To me, it seems kind of dodgy. To remove that possibility making it <code>final</code> is a smart move.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Later on, we will look at how this Flow can be subclassed.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Below is the <code>SendMessageResponder</code> that interacts with the <code>SendMessageFlow</code>. It follows the same concepts as above and therefore I will only show it as a reference for later:</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>[gist https://gist.github.com/lankydan/ed4c7336cd1bf344770b4dfa68916951 /]</p>
<!-- /wp:paragraph -->

<!-- wp:heading {"level":3} -->
<h3>Extending an existing Initiating Flow</h3>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>In this section, we get to see how the developer can make use of the work done on the previous Flow. It already has all the needed functionality. The only thing that is missing is the small amount of extra traceability that the developer wants to add in. Thanks to the functions added to the base Flow. This should cause no problems.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Let's start with extending an Initiating Flow. The requirements for doing so are as follows:</p>
<!-- /wp:paragraph -->

<!-- wp:list -->
<ul><li>Extend the base <code>@InitiatingFlow</code></li><li>Do <strong>not</strong> add <code>@InitiatingFlow</code> to the new Flow (errors will occur if you do)</li><li>Reference the base Flow's constructor (<code>super</code> in Java)</li><li>Override any desired functions</li><li>Call the new Flow instead of the base Flow</li></ul>
<!-- /wp:list -->

<!-- wp:paragraph -->
<p>After reading that list, you might have realised that this is pretty much a description of inheritance in Object Oriented languages (like Kotlin and Java). There might be more going on inside Corda to allow this to work but from your perspective, you are just writing normal Object Oriented code like usual.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Taking these requirements we can see what an extended Flow might look like:</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>[gist https://gist.github.com/lankydan/8e3994ae297ed8b58d4e09f19de1c892 /]</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>I have left in all the noisy functions that implement the extra traceability I was talking about, but that is due to how empty the class would be without them. Since <code>call</code> does not need to be implemented. This Flow only needs to override the <code>open</code> functions. To be honest, it doesn't <em>need</em> to override them at all, they are optional. If desired, this Flow could override a single function and then be left empty.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Have all the requirements listed above been met?</p>
<!-- /wp:paragraph -->

<!-- wp:list -->
<ul><li><code>CassandraSendMessageFlow</code> extends <code>SendMessageFlow</code></li><li>There is no <code>@InitiatingFlow</code> in sight</li><li>In Kotlin you must call the <code>super</code> constructor anyway, so that's done</li><li>In this scenario, all the functions have overridden</li><li>We haven't got this far</li></ul>
<!-- /wp:list -->

<!-- wp:paragraph -->
<p>Ok, so that is 4/5 so far. That's a pretty good start. To cross off the last item on the list we need to see how it's called. Below are snippets that call the base <code>SendMessageFlow</code> and the <code>CassandraSendMessageFlow</code> extending Flow.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Starting with <code>SendMessageFlow</code>:</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>[gist https://gist.github.com/lankydan/f4a099abbb60318e28ac9c146e7b1f78 /]</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Followed by <code>CassandraSendMessageFlow</code>:</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>[gist https://gist.github.com/lankydan/9f08f6e99723bd6ad505d826f95518b8 /]</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Notice the difference? In this scenario, only the name of the Flow has changed. Nothing else.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Both snippets are completely valid. Calling the original <code>SendMessageFlow</code> is still allowed. Remember from our perspective, it is just a normal Object Oriented code. It won't have the fancy extra code added to the extending Flow but it will still execute without issues. Completing this step meets the last requirement for extending an <code>@InitiatingFlow</code>.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Before we end this section, here is an important piece of information to remember from the <a href="https://docs.corda.net/head/flow-overriding.html#configuring-responder-flows" target="_blank" rel="noreferrer noopener" aria-label="Corda documentation (opens in a new tab)">Corda documentation</a>: </p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p><strong><em>"You must ensure the sequence of sends/receives/subFlows in a subclass are compatible with the parent."</em></strong></p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>I will put this into all of the following sections since failing to follow this will cause your Flows to fail.</p>
<!-- /wp:paragraph -->

<!-- wp:heading {"level":3} -->
<h3>Extending a Responder Flow</h3>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>Extending a Responder Flow works in a very similar way to extending an <code>@InitiatingFlow</code> Flow. The only difference is how it is called. As stated in the <a href="https://docs.corda.net/head/flow-overriding.html#subclassing-a-flow" target="_blank" rel="noreferrer noopener" aria-label="documentation (opens in a new tab)">documentation</a>: </p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p><strong><em>"Corda would detect that both </em></strong><code><strong><em>BaseResponder</em></strong></code><strong><em> and </em></strong><code><strong><em>SubResponder</em></strong></code><strong><em> are configured for responding to Initiator. Corda will then calculate the hops to </em></strong><code><strong><em>FlowLogic</em></strong></code><strong><em> and select the implementation which is the furthest distance, ie: the most subclassed implementation."</em></strong></p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>The statement, "most subclassed" is the important takeaway from this text. Therefore from a developer's viewpoint, all they need to do is extend the external base Responder Flow and that's it. I quite liked previous the requirements list, so let's go through another one for extending Responder Flows:</p>
<!-- /wp:paragraph -->

<!-- wp:list -->
<ul><li>Extend the base <code>@InitiatedBy</code> / Responder Flow</li><li>Add <code>@InitiatedBy</code> to the new Flow</li><li>Reference the base Flow's constructor (<code>super</code> in Java)</li><li>Override any desired functions</li></ul>
<!-- /wp:list -->

<!-- wp:paragraph -->
<p>If you are vigilant, you might have noticed that there is no mention of how to call it. The extending Responder Flow does not need to be called or referenced anywhere else. Corda will do the work to route everything to the right location.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Just to be sure, let's have a quick look at an example:</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>[gist https://gist.github.com/lankydan/3c621b023679ffdc4f139c99e47df816 /]</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Furthermore, let's look back at the statement "most subclassed" again. The <code>CassandraSendMessageResponder</code> is a subclass of <code>SendMessageResponder</code> and is therefore chosen by Corda to handle requests from the Initiating Flow. But, this could be taken a step further. If there was another class, say <code>SuperSpecialCassandraSendMessageResponder</code>, this Flow is now what Corda will start using. Although I do find this sort of scenario somewhat unlikely at the moment, it is definitely worth knowing about.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Copying and pasting this statement again so you don't forget: </p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p><strong><em>"You must ensure the sequence of sends/receives/subFlows in a subclass are compatible with the parent."</em></strong></p>
<!-- /wp:paragraph -->

<!-- wp:heading {"level":3} -->
<h3>Overriding a Responder Flow</h3>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>This is purposely a separate section. Here we will talk specifically about overriding a Responder Flow rather than extending one. Why would you do this and what is the difference? Answering the first question, a developer may want to write a Responder Flow that greatly diverges from the original base Flow but still needs to interact with the specific Initiating Flow provided by an external CorDapp. To achieve this they can override the Flow. Another word to describe this could be "replace". The original base Flow is completely replaced by the overriding Flow. There is no involvement of extension in this situation.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>I think the <a href="https://docs.corda.net/head/flow-overriding.html#overriding-a-flow-via-node-configuration" target="_blank" rel="noreferrer noopener" aria-label="Corda documentation's (opens in a new tab)">Corda documentation's</a> wording on this subject is quite good: </p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p><em><strong>"Whilst the subclassing approach is likely to be useful for most applications, there is another mechanism to override this behaviour. This would be useful if, for example, a specific CordApp user requires such a different responder that subclassing an existing flow would not be a good solution."</strong></em></p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Hopefully, this extract along with my earlier description will clarify the difference between extending and overriding Responder Flows.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>So, what might an overriding Flow look like? Well, anything you want really, within reason. Maybe it might look like the below, although I doubt it:</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>[gist https://gist.github.com/lankydan/e84bbb807801ff7cbf39ac085e0d61d7 /]</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Since this Flow is completely replacing the original base Flow, it will look just like a normal Responder Flow. Since, well, it is one. That means it has <code>@InitiatedBy</code> referencing the Initiating Flow, extends <code>FlowLogic</code> and implements the <code>call</code> function.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Just putting this here one last time:</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p><em><strong>"You must ensure the sequence of sends/receives/subFlows in a subclass are compatible with the parent."</strong></em></p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>This is even more prevalent here than in the previous sections. Since the whole <code>call</code> function is being overridden you must make sure that every <code>send</code> and <code>receive</code> is in the right place so interactions with the Initiating Flow run without errors.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Configuration wise there is a bit more to do than with extending a Flow. In this situation, we are trying to completely replace a Responder with another. To do so, we need a way to tell the node to redirect interactions from an Initiating Flow to a new overriding Responder Flow. Corda provides a way to do just that.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>To specify the redirection add the following to your <code>node.conf</code>:</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>[gist https://gist.github.com/lankydan/928d740bee34f10b2142ee98a1954a69 /]</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Obviously, change the classes referenced to your own...</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>So what is going on here? The config says that the <code>SendMessageFlow</code> which normally interacts with <code>SendMessageResponder</code> will now route to <code>OverridingResponder</code> instead.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>To make everything a bit easier as well, the <code>Cordform</code> plugin provides the <code>flowOverride</code> method as part of <code>deployNodes</code>. This will then generate the configuration block above for you. For the example above, the following code was used:</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>[gist https://gist.github.com/lankydan/100455bc79ef8ea030d2803d36d3ba06 /]</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Now, after <code>deployNodes</code> has run and you have started your node, any requests coming from <code>SendMessageFlow</code> or any of its subclasses will now route communication to the <code>OverridingResponder</code>.</p>
<!-- /wp:paragraph -->

<!-- wp:heading {"level":3} -->
<h3>Conclusion</h3>
<!-- /wp:heading -->

<!-- wp:paragraph -->
<p>One of the handy features that Corda 4 provides is the ability to customise Flows from third-party CorDapps (or your own). This is done by two methods, extending or overriding.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Extending would be my first choice between the two but it does require a bit more effort on the side of the CorDapp developer. They must provide enough avenues for customisation without relinquishing control of the original functionality of their Flows. Providing little customisation might not deter other developers from using their CorDapp. But, developers could become unhappy with the lack of control of their own application. It is a slippery slope to control original intent with routes for customisation. On the other hand, actually extending a Flow does not require much work, making it easier for developers to adopt and adapt external Flows.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>Overriding, on the other hand, requires no work for a CorDapp developer and instead everything is put onto the developer leveraging external Responder Flows. That is because the existing Flow is pretty much being thrown away and the only reference back to the original implementation is the link to the Initiating Flow.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>By embracing both the extending and overriding of Flows, CorDapp developers will be able to leverage external CorDapps while still providing enough customisation to fulfil all business requirements they might have. As time progresses, developers will drive the adoption of reusing existing CorDapps as they provide access to additional customisation, soon taking the same position as Open Source libraries that we all already leverage in any work we do.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>The code used in this post can be found on my <a href="https://github.com/lankydan/corda-extendable-flows" target="_blank" rel="noreferrer noopener" aria-label="GitHub (opens in a new tab)">GitHub</a>. It contains the code for <code>CassandraSendMessageFlow</code> which sets up a connection to an external Cassandra database to save tracing style data. It also contains another module that sends HTTP requests as part of its extension of the base Flows. If you are still curious after reading this post, this repository might help.</p>
<!-- /wp:paragraph -->

<!-- wp:paragraph -->
<p>If you enjoyed this post or found it helpful (or both) then please feel free to follow me on Twitter at <a href="https://twitter.com/LankyDanDev" target="_blank" rel="noreferrer noopener" aria-label="@LankyDanDev (opens in a new tab)">@LankyDanDev</a> and remember to share with anyone else who might find this useful!</p>
<!-- /wp:paragraph -->