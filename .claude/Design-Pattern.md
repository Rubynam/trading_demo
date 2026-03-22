# Domain-Driven Hexagon


# Application Core

This is the core of the system which is built using 

**Domain layer**:

- Entities
- Aggregates
- Domain Services
- Value Objects
- Domain Errors

**Application layer**:

- Application Services
- Commands and Queries
- Ports

**Note**: different implementations may have slightly different layer structures depending on applications needs. Also, more layers and building blocks may be added if needed.

---

# Application layer

## Application Services

Application Services (also called "Workflow Services", "Use Cases", "Interactors", etc.) are used to orchestrate the steps required to fulfill the commands imposed by the client.

Application services:

- Typically used to orchestrate how the outside world interacts with your application and performs tasks required by the end users;
- Contain no domain-specific business logic;
- Operate on scalar types, transforming them into Domain types. A scalar type can be considered any type that's unknown to the Domain Model. This includes primitive types and types that don't belong to the Domain;
- Uses ports to declare dependencies on infrastructural services/adapters required to execute domain logic (ports are just interfaces, we will discuss this topic in details below);
- Fetch domain `Entities`/`Aggregates` (or anything else) from database/external APIs (through ports/interfaces, with concrete implementations injected by the [DI];
- Execute domain logic on those `Entities`/`Aggregates` (by invoking their methods);
- In case of working with multiple `Entities`/`Aggregates`, use a `Domain Service` to orchestrate them;
- Execute other out-of-process communications through Ports (like event emits, sending emails, etc.);
- Services can be used as a `Command`/`Query` handlers;
- Should not depend on other application services since it may cause problems (like cyclic dependencies);

One service per use case is considered a good practice.

<details>
<summary>What are "Use Cases"?</summary>


> In software and systems engineering, a use case is a list of actions or event steps typically defining the interactions between a role (known in the Unified Modeling Language as an actor) and a system to achieve a goal.

Use cases are, simply said, list of actions required from an application.

---

</details>

## Commands and Queries

This principle is called . When possible, methods should be separated into `Commands` (state-changing operations) and `Queries` (data-retrieval operations). To make a clear distinction between those two types of operations, input objects can be represented as `Commands` and `Queries`. Before DTO reaches the domain, it's converted into a `Command`/`Query` object.

### Commands

`Command` is an object that signals user intent, for example `CreateUserCommand`. It describes a single action (but does not perform it).

`Commands` are used for state-changing actions, like creating new user and saving it to the database. Create, Update and Delete operations are considered as state-changing.

Data retrieval is responsibility of `Queries`, so `Command` methods should not return business data.

Some CQS purists may say that a `Command` shouldn't return anything at all. But you will need at least an ID of a created item to access it later. To achieve that you can let clients generate a [UUID].

Though, violating this rule and returning some metadata, like `ID` of a created item, redirect link, confirmation message, status, or other metadata is a more practical approach than following dogmas.


To execute a command you can use a `Command Bus` instead of importing a service directly. This will decouple a command Invoker from a Receiver, so you can send your commands from anywhere without creating coupling.

Avoid command handlers executing other commands in this fashion: Command → Command. Instead, use events for that purpose, and execute next commands in a chain in an Event handler: Command → Event → Command.

### Queries

`Query` is similar to a `Command`. It belongs to a read model and signals user intent to find something and describes how to do it.

`Query` is just a data retrieval operation and should not make any state changes (like writes to the database, files, third party APIs, etc.). For this reason, in read model we can bypass a domain and repository layers completely and query database directly from a query handler.

Similarly to Commands, Queries can use a `Query Bus` if needed. This way you can query anything from anywhere without importing classes directly and avoid coupling.



By enforcing `Command` and `Query` separation, the code becomes simpler to understand. One changes something, another just retrieves data.

Also, following CQS from the start will facilitate separating write and read models into different databases if someday in the future the need for it arises.



## Ports

Ports are interfaces that define contracts that should be implemented by adapters. For example, a port can abstract technology details (like what type of database is used to retrieve some data), and infrastructure layer can implement an adapter in order to execute some action more related to technology details rather than business logic.

In Application Core **dependencies point inwards**. Outer layers can depend on inner layers, but inner layers never depend on outer layers. Application Core shouldn't depend on frameworks or access external resources directly. Any external calls to out-of-process resources/retrieval of data from remote processes should be done through `ports` (interfaces), with class implementations created somewhere in infrastructure layer and injected into application's core ([Dependency Injection] and [Dependency Inversion]). This makes business logic independent of technology, facilitates testing, allows to plug/unplug/swap any external resources easily making application modular and [loosely coupled].

- Ports are basically just interfaces that define what has to be done and don't care about how it's done.
- Ports can be created to abstract side effects like I/O operations and database access, technology details, invasive libraries, legacy code etc. from the Domain.
- By abstracting side effects, you can test your application logic in isolation by [mocking] the implementation. This can be useful for [unit testing].
- Ports should be created to fit the Domain needs, not simply mimic the tools APIs.
- Mock implementations can be passed to ports while testing. Mocking makes your tests faster and independent of the environment.
- Abstraction provided by ports can be used to inject different implementations to a port if needed ([polymorphism]).
- Ports can also help to delay decisions. The Domain layer can be implemented even before deciding what technologies (frameworks, databases etc.) will be used.


# Domain Layer

This layer contains the application's business rules.


> Domain entities should always be valid entities. There are a certain number of invariants for an object that should always be true. For example, an order item object always has to have a quantity that must be a positive integer, plus an article name and price. Therefore, invariants enforcement is the responsibility of the domain entities (especially of the aggregate root) and an entity object should not be able to exist without being valid.

Entities:

- Contain Domain business logic. Avoid having business logic in your services when possible, this leads to [Anemic Domain Model]) (Domain Services are an exception for business logic that can't be put in a single entity).
- Have an identity that defines it and makes it distinguishable from others. Its identity is consistent during its life cycle.
- Equality between two entities is determined by comparing their identificators (usually its `id` field).
- Can contain other objects, such as other entities or value objects.
- Are responsible for collecting all the understanding of state and how it changes in the same place.
- Responsible for the coordination of operations on the objects it owns.
- Know nothing about upper layers (services, controllers etc.).
- Domain entities data should be modelled to accommodate business logic, not some database schema.
- Entities must protect their invariants, try to avoid public setters - update state using methods and execute invariant validation on each update if needed (this can be a simple `validate()` method that checks if business rules are not violated by update).
- Must be consistent on creation. Validate Entities and other domain objects on creation and throw an error on first failure. [Fail Fast]
- Make Entities partially immutable. Identify what properties shouldn't change after creation and make them `readonly` (for example `id` or `createdAt`).


## Aggregates

[Aggregate] is a cluster of domain objects that can be treated as a single unit. It encapsulates entities and value objects which conceptually belong together. It also contains a set of operations which those domain objects can be operated on.

- Aggregates help to simplify the domain model by gathering multiple domain objects under a single abstraction.
- Aggregates should not be influenced by the data model. Associations between domain objects are not the same as database relationships.
- Aggregate root is an entity that contains other entities/value objects and all logic to operate them.
- Aggregate root has global identity ([UUID / GUID]/ primary key). Entities inside the aggregate boundary have local identities, unique only within the Aggregate.
- Aggregate root is a gateway to entire aggregate. Any references from outside the aggregate should **only** go to the aggregate root.
- Only Aggregate Roots can be obtained directly with database queries. Everything else must be done through traversal.
- Similar to `Entities`, aggregates must protect their invariants through entire lifecycle. When a change to any object within the Aggregate boundary is committed, all invariants of the whole Aggregate must be satisfied. Simply said, all objects in an aggregate must be consistent, meaning that if one object inside an aggregate changes state, this shouldn't conflict with other domain objects inside this aggregate (this is called _Consistency Boundary_).
- Objects within the Aggregate can reference other Aggregate roots via their globally unique identifier (id). Avoid holding a direct object reference.
- Try to avoid aggregates that are too big, this can lead to performance and maintaining problems.
- Aggregates can publish `Domain Events` (more on that below).

## Domain Events

Domain Event indicates that something happened in a domain that you want other parts of the same domain (in-process) to be aware of. Domain events are just messages pushed to an in-memory Domain Event dispatcher.

## Integration Events

Out-of-process communications (calling microservices, external APIs) are called `Integration Events`. If sending a Domain Event to external process is needed then domain event handler should send an `Integration Event`.

Integration Events usually should be published only after all Domain Events finished executing and saving all changes to the database.

To handle integration events in microservices you may need an external message broker / event bus like  [Kafka]

---

## Domain Services

Eric Evans, Domain-Driven Design:

> Domain services are used for "a significant process or transformation in the domain that is not a natural responsibility of an ENTITY or VALUE OBJECT"

- Domain Service is a specific type of domain layer class that is used to execute domain logic that relies on two or more `Entities`.
- Domain Services are used when putting the logic on a particular `Entity` would break encapsulation and require the `Entity` to know about things it really shouldn't be concerned with.
- Domain services are very granular, while application services are a facade purposed with providing an API.
- Domain services operate only on types belonging to the Domain. They contain meaningful concepts that can be found within the Ubiquitous Language. They hold operations that don't fit well into Value Objects or Entities.


## Domain Invariants

Domain [invariants]are the policies and conditions that are always met for the Domain in particular context. Invariants determine what is possible or what is prohibited in the context.

Invariants enforcement is the responsibility of domain objects (especially of the entities and aggregate roots).

There are a certain number of invariants for an object that should always be true. For example:

- When sending money, amount must always be a positive integer, and there always must be a receiver credit card number in a correct format;
- Client cannot purchase a product that is out of stock;
- Client's wallet cannot have less than 0 balance;
- etc.

If the business has some rules similar to described above, the domain object should not be able to exist without following those rules.

Below we will discuss some validation techniques for your domain objects.

### Make illegal states unrepresentable

Use Value Objects/Domain Primitives and Types  to make illegal states impossible to represent in your program.

Some people recommend using objects for every value:


> Making illegal states unrepresentable is all about statically proving that all runtime values (without exception) correspond to valid objects in the business domain. The effect of this technique on eliminating meaningless runtime states is astounding and cannot be overstated.

Let's distinguish two types of protection from illegal states: at **compile time** and at **runtime**.

#### Validation at compile time

Types give useful semantic information to a developer. Good code should be easy to use correctly, and hard to use incorrectly. Types system can be a good help for that. It can prevent some nasty errors at compile time, so IDE will show type errors right away.

The simplest example may be using enums instead of constants, and use those enums as input type for something. When passing anything that is not intended the IDE will show a type error:

Handle Global Exception in validate state.

Now only either `Email`, or `Phone`, or both must be provided. If nothing is provided, the IDE will show a type error right away. Now business rule validation is moved from runtime to **compile time**, which makes the application more secure and gives a faster feedback when something is not used as intended.

This is called a _typestate pattern_.

> The typestate pattern is an API design pattern that encodes information about an object’s run-time state in its compile-time type.

#### Validation at runtime

Data should not be trusted. There are a lot of cases when invalid data may end up in a domain. For example, if data comes from external API, database, or if it's just a programmer error.

Things that can't be validated at compile time (like user input) are validated at runtime.

First line of defense is validation of user input DTOs.

Second line of defense are Domain Objects. Entities and value objects have to protect their invariants. Having some validation rules here will protect their state from corruption. You can use techniques like [Design by contract] by defining preconditions in object constructors and checking postconditions and invariants before saving an object to the database.

Enforcing self-validation of your domain objects will inform immediately when data is corrupted. Not validating domain objects allows them to be in an incorrect state, this leads to problems.

By combining compile and runtime validations, using objects instead of primitives, enforcing self-validation and invariants of your domain objects, using Design by contract, [Algebraic Data Types (ADT)] and typestate pattern, and other similar techniques, you can achieve an architecture where it's hard, or even impossible, to end up in illegal states, thus improving security and robustness of your application dramatically (at a cost of extra boilerplate code).

**Recommended to read**:

- [Backend Best Practices: Data Validation]

### Guarding vs validating

You may have noticed that we do validation in multiple places:

1. First when user input is sent to our application. In our example we use DTO decorators: [create-user.request-dto.ts].
2. Second time in domain objects, for example: [address.value-object.ts].

So, why are we validating things twice? Let's call a second validation "_guarding_", and distinguish between guarding and validating:

- Guarding is a failsafe mechanism. Domain layer views it as invariants to comply with always-valid domain model.
- Validation is a filtration mechanism. Outside layers view them as input validation rules.

> This difference leads to different treatment of violations of these business rules. An invariant violation in the domain model is an exceptional situation and should be met with throwing an exception. On the other hand, there’s nothing exceptional in external input being incorrect.

The input coming from the outside world should be filtered out before passing it further to the domain model. It’s the first line of defense against data inconsistency. At this stage, any incorrect data is denied with corresponding error messages.
Once the filtration has confirmed that the incoming data is valid it's passed to a domain. When the data enters the always-valid domain boundary, it's assumed to be valid and any violation of this assumption means that you’ve introduced a bug.
Guards help to reveal those bugs. They are the failsafe mechanism, the last line of defense that ensures data in the always-valid boundary is indeed valid. Guards comply with the [Fail Fast principle](https://enterprisecraftsmanship.com/posts/fail-fast-principle) by throwing runtime exceptions.

Domain classes should always guard themselves against becoming invalid.

For preventing null/undefined values, empty objects and arrays, incorrect input length etc. 


**Keep in mind** that not all validations/guarding can be done in a single domain object, it should validate only rules shared by all contexts. There are cases when validation may be different depending on a context, or one field may involve another field, or even a different entity. Handle those cases accordingly.

<details>
<summary><b>Note</b>: Using validation library instead of custom guards</summary>

Instead of using custom _guards_ you could use an external validation library, but it's not a good practice to tie domain to external libraries and is not usually recommended.

Although exceptions can be made if needed, especially for very specific validation libraries that validate only one thing (like specific IDs, for example bitcoin wallet address). Tying only one or just few `Value Objects` to such a specific library won't cause any harm. Unlike general purpose validation libraries which will be tied to domain everywhere, and it will be troublesome to change it in every `Value Object` in case when old library is no longer maintained, contains critical bugs or is compromised by hackers etc.

Though, it's fine to do full sanity checks using validation framework or library **outside** the domain (for example [class-validator](https://www.npmjs.com/package/class-validator) decorators in `DTOs`), and do only some basic checks (guarding) inside of domain objects (besides business rules), like checking for `null` or `undefined`, checking length, matching against simple regexp etc. to check if value makes sense and for extra security.

<details>
<summary>Note about using regexp</summary>

Be careful with custom regexp validations for things like validating `email`, only use custom regexp for some very simple rules and, if possible, let validation library do its job on more difficult ones to avoid problems in case your regexp is not good enough.

Also, keep in mind that custom regexp that does same type of validation that is already done by validation library outside of domain may create conflicts between your regexp and the one used by a validation library.

For example, value can be accepted as valid by a validation library, but `Value Object` may throw an error because custom regexp is not good enough (validating `email` is more complex than just copy - pasting a regular expression found in google. Though, it can be validated by a simple rule that is true all the time and won't cause any conflicts, like every `email` must contain an `@`). Try finding and validating only patterns that won't cause conflicts.

---

</details>

Although there are other strategies on how to do validation inside domain, like passing validation schema as a dependency when creating new `Value Object`, but this creates extra complexity.

Either to use external library/framework for validation inside domain or not is a tradeoff, analyze all the pros and cons and choose what is more appropriate for current application.

For some projects, especially smaller ones, it might be easier and more appropriate to just use validation library/framework.

</details>

## Domain Errors

Application's core and domain layers shouldn't throw HTTP exceptions or statuses since it shouldn't know in what context it's used, since it can be used by anything: HTTP controller, Microservice event handler, Command Line Interface etc. A better approach is to create custom error classes with appropriate error codes.

Exceptions are for exceptional situations. Complex domains usually have a lot of errors that are not exceptional, but a part of a business logic (like "seat already booked, choose another one"). Those errors may need special handling. In those cases returning explicit error types can be a better approach than throwing.

Returning an error instead of throwing explicitly shows a type of each exception that a method can return so you can handle it accordingly. It can make an error handling and tracing easier.

To help with that you can create an [Algebraic Data Types (ADT)] for your errors and use some kind of Result object type with a Success or a Failure condition from functional languages similar to Haskell or Scala). Unlike throwing exceptions, this approach allows defining types (ADTs) for every error and will let you see and handle them explicitly instead of using `try/catch` and avoid throwing exceptions that are invisible at compile time.


This approach gives us a fixed set of expected error types, so we can decide what to do with each:

Throwing makes errors invisible for the consumer of your functions/methods (until those errors happen at runtime, or until you dig deeply into the source code and find them). This means those errors are less likely to be handled properly.

Returning errors instead of throwing them adds some extra boilerplate code, but can make your application robust and secure since errors are now explicitly documented and visible as return types. You decide what to do with each error: propagate it further, transform it, add extra metadata, or try to recover from it (for example, by retrying the operation).

**Warning**: Some errors/exceptions are non-recoverable and should be thrown, not returned. If you return technical Exceptions (like connection failed, process out of memory, etc.), It may cause some security issues and goes against [Fail-fast]principle. Instead of terminating a program flow immediately and logging the error, returning an exception continues program execution and allows it to run in an incorrect state, which may lead to more unexpected errors, so it's generally better to throw an Exception in those cases rather than returning it. Analyze if the error is "likely recoverable" or "likely unrecoverable". If an error is most likely a recoverable error, it's a great candidate for using it in a Result object. If an error is most likely unrecoverable, throw it.

## Using libraries inside Application's core

Whether to use libraries in application core and especially domain layer is a subject of a lot of debates. In real world, injecting every library instead of importing it directly is not always practical, so exceptions can be made for some single responsibility libraries that help to implement domain logic (like working with numbers).

Main recommendations to keep in mind is that libraries imported in application's core **shouldn't** expose:

- Functionality to access any out-of-process resources (http calls, database access etc);
- Functionality not relevant to domain (frameworks, technology details like ORMs, Logger etc.).
- Functionality that brings randomness (generating random IDs, timestamps etc.) since this makes tests unpredictable (though in TypeScript world it's not that big of a deal since this can be mocked by a test library without using DI);
- If a library changes often or has a lot of dependencies of its own it most likely shouldn't be used in domain layer.

To use such libraries consider creating an `anti-corruption` layer by using [adapter] or [facade] patterns.

We sometimes tolerate libraries in the center, but be careful with general purpose libraries that may scatter across many domain objects. It will be hard to replace those libraries if needed. Tying only one or just a few domain objects to some single-responsibility library should be fine. It's way easier to replace a specific library that is tied to one or few objects than a general purpose library that is everywhere.

In addition to different libraries there are Frameworks. Frameworks can be a real nuisance, because by definition they want to be in control, and it's hard to replace a Framework later when your entire application is glued to it. It's fine to use Frameworks in outside layers (like infrastructure), but keep your domain clean of them when possible. You should be able to extract your domain layer and build a new infrastructure around it using any other framework without breaking your business logic.

NestJS does a good job, as it uses decorators which are not very intrusive, so you could use decorators like `@Inject()` without affecting your business logic at all, and it's relatively easy to remove or replace it when needed. Don't give up on frameworks completely, but keep them in boundaries and don't let them affect your business logic.

Offload as much of irrelevant responsibilities as possible from the core, especially from domain layer. In addition, try to minimize usage of dependencies in general. More dependencies your software has means more potential errors and security holes. One technique for making software more robust is to minimize what your software depends on - the less that can go wrong, the less will go wrong. On the other hand, removing all dependencies would be counterproductive as replicating that functionality would require huge amount of work and would be less reliable than just using a popular, battle-tested library. Finding a good balance is important, this skill requires experience.

---

# Interface Adapters

Interface adapters (also called driving/primary adapters) are user-facing interfaces that take input data from the user and repackage it in a form that is convenient for the use cases(services/command handlers) and entities. Then they take the output from those use cases and entities and repackage it in a form that is convenient for displaying it back for the user. User can be either a person using an application or another server.

Contains `Controllers` and `Request`/`Response` DTOs (can also contain `Views`, like backend-generated HTML templates, if required).

## Controllers

- Controller is a user-facing API that is used for parsing requests, triggering business logic and presenting the result back to the client.
- One controller per use case is considered a good practice.
- In world controllers may be a good place to use [OpenAPI/Swagger decorators] for documentation.

One controller per trigger type can be used to have a clearer separation. For example:


### Resolvers

If you are using Rest API , instead of controllers, you will use.

One of the main benefits of a layered architecture is separation of concerns. As you can see, it doesn't matter if you use [REST], the only thing that changes is user-facing API layer (interface-adapters). All the application Core stays the same since it doesn't depend on technology you are using.

---

## DTOs

Data that comes from external applications should be represented by a special type of classes - Data Transfer Objects ([DTO] for short).
Data Transfer Object is an object that carries data between processes. It defines a contract between your API and clients.

### Request DTOs

Input data sent by a user.

- Using Request DTOs gives a contract that a client of your API has to follow to make a correct request.

### Response DTOs

Output data returned to a user.

- Using Response DTOs ensures clients only receive data described in DTOs contract, not everything that your model/entity owns (which may result in data leaks).

---

DTO contracts protect your clients from internal data structure changes that may happen in your API. When internal data models change (like renaming variables or splitting tables), they can still be mapped to match a corresponding DTO to maintain compatibility for anyone using your API.

When updating DTO interfaces, a new version of API can be created by prefixing an endpoint with a version number, for example: `v2/users`. This will make transition painless by preventing breaking compatibility for users that are slow to update their apps that uses your API.

You may have noticed that our contains the same properties.
So why do we need DTOs if we already have Command objects that carry properties? Shouldn't we just have one class to avoid duplication?

> Because commands and DTOs are different things, they tackle different problems. Commands are serializable method calls - calls of the methods in the domain model. Whereas DTOs are the data contracts. The main reason to introduce this separate layer with data contracts is to provide backward compatibility for the clients of your API. Without the DTOs, the API will have breaking changes with every modification of the domain model.


### Additional recommendations

- DTOs should be data-oriented, not object-oriented. Its properties should be mostly primitives. We are not modeling anything here, just sending flat data around.
- When returning a `Response` prefer _whitelisting_ properties over _blacklisting_. This ensures that no sensitive data will leak in case if programmer forgets to blacklist newly added properties that shouldn't be returned to the user.
- If you use the same DTOs in multiple apps (frontend and backend, or between microservices), you can keep them somewhere in a shared directory instead of module directory and create a git submodule or a separate package for sharing them.
- `Request`/`Response` DTO classes may be a good place to use validation and sanitization decorators  (make sure that all validation errors are gathered first and only then return them to the user, this is called [Notification pattern]. Class-validator does this by default).
- `Request`/`Response` DTO classes may also be a good place to use Swagger/OpenAPI library decorators.
- If DTO decorators for validation/documentation are not used, DTO can be just an interface instead of a class.
- Data can be transformed to DTO format using a separate mapper or right in the constructor of a DTO class.

### Local DTOs

Another thing that can be seen in some projects is local DTOs. Some people prefer to never use domain objects (like entities) outside its domain (in `controllers`, for example) and return a plain DTO object instead. This project doesn't use this technique, to avoid extra complexity and boilerplate code like interfaces and data mapping.
are Martin Fowler's thoughts on local DTOs, in short (quote):

> Some people argue for them (DTOs) as part of a Service Layer API because they ensure that service layer clients aren't dependent upon an underlying Domain Model. While that may be handy, I don't think it's worth the cost of all of that data mapping.

Though you may want to introduce Local DTOs when you need to decouple modules properly. For example, when querying from one module to another you don't want to leak your entities between modules. In that case using a Local DTO may be justified.

---

# Infrastructure layer

The Infrastructure layer is responsible for encapsulating technology. You can find there the implementations of database repositories for storing/retrieving business entities, message brokers to emit messages/events, I/O services to access external resources, framework related code and any other code that represents a replaceable detail for the architecture.

It's the most volatile layer. Since the things in this layer are so likely to change, they are kept as far away as possible from the more stable domain layers. Because they are kept separate, it's relatively easy to make changes or swap one component for another.

Infrastructure layer can contain `Adapters`, database related files like `Repositories`, `ORM entities`/`Schemas`, framework related files etc.

## Adapters

- Infrastructure adapters (also called driven/secondary adapters) enable a software system to interact with external systems by receiving, storing and providing data when requested (like persistence, message brokers, sending emails or messages, requesting 3rd party APIs etc).
- Adapters also can be used to interact with different domains inside single process to avoid coupling between those domains.
- Adapters are essentially an implementation of ports. They are not supposed to be called directly in any point in code, only through ports(interfaces).
- Adapters can be used as Anti-Corruption Layer (ACL) for legacy code.


Adapters should have:

- a `port` somewhere in application/domain layer that it implements;
- a mapper that maps data **from** and **to** domain (if it's needed);
- a DTO/interface for received data;
- a validator to make sure incoming data is not corrupted (validation can reside in DTO class using decorators, or it can be validated by `Value Objects`).

## Repositories

Repositories are abstractions over collections of entities that are living in a database.
They centralize common data access functionality and encapsulate the logic required to access that data. Entities/aggregates can be put into a repository and then retrieved at a later time without domain even knowing where data is saved: in a database, in a file, or some other source.

We use repositories to decouple the infrastructure or technology used to access databases from the domain model layer.

Martin Fowler describes a repository as follows:

> A repository performs the tasks of an intermediary between the domain model layers and data mapping, acting similarly to a set of domain objects in memory. Client objects declaratively build queries and send them to the repositories for answers. Conceptually, a repository encapsulates a set of objects stored in the database and operations that can be performed on them, providing a way that is closer to the persistence layer. Repositories, also, support the purpose of separating, clearly and in one direction, the dependency between the work domain and the data allocation or mapping.

The data flow here looks something like this: repository receives a domain `Entity` from application service, maps it to database schema/ORM format, does required operations (saving/updating/retrieving etc), then maps it back to domain `Entity` format and returns it back to service.

Application's core usually is not allowed to depend on repositories directly, instead it depends on abstractions (ports/interfaces). This makes data retrieval technology-agnostic.

**Note**: in theory, most publications out there recommend abstracting a database with interfaces. In practice, it's not always useful. Most of the projects out there never change database technology (or rewrite most of the code anyway if they do). Another downside is that if you abstract a database you are more likely not using its full potential. This project abstracts repositories with a generic port to make a practical, but this doesn't mean you should do that too. Think carefully before using abstractions. 

Example files:

This project contains abstract repository class that allows to make basic CRUD operations:. This base class is then extended by a specific repository, and all specific operations that an entity may need are implemented in that specific repo.

## Persistence models

Using a single entity for domain logic and database concerns leads to a database-centric architecture. In DDD world domain model and persistence model should be separated.

Since domain `Entities` have their data modeled so that it best accommodates domain logic, it may be not in the best shape to save in a database. For that purpose `Persistence models` can be created that have a shape that is better represented in a particular database that is used. Domain layer should not know anything about persistence models, and it should not care.

There can be multiple models optimized for different purposes, for example:

- Domain with its own models - `Entities`, `Aggregates` and `Value Objects`.
- Persistence layer with its own models - ORM, schemas, read/write models if databases are separated into a read and write db  etc.

Over time, when the amount of data grows, there may be a need to make some changes in the database like improving performance or data integrity by re-designing some tables or even changing the database entirely. Without an explicit separation between `Domain` and `Persistance` models any change to the database will lead to change in your domain `Entities` or `Aggregates`. For example, when performing a database [normalization]() data can spread across multiple tables rather than being in one table, or vice-versa for [denormalization]. This may force a team to do a complete refactoring of a domain layer which may cause unexpected bugs and challenges. Separating Domain and Persistence models prevents that.

**Note**: separating domain and persistence models may be overkill for smaller applications. It requires a lot of effort creating and maintaining boilerplate code like mappers and abstractions. Consider all pros and cons before making this decision.
