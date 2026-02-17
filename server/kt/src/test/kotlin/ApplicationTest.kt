package com.cherba29.tally

import com.expediagroup.graphql.server.types.GraphQLRequest
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.jackson.jackson
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.testing.testApplication
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

class ApplicationTest : DescribeSpec({
  describe("routing") {
    it("root") {
      testApplication {
        application {
          graphQLModule()
        }
        val response = client.get("/")
        response.status shouldBe HttpStatusCode.OK
        response.bodyAsText() shouldBe "Hello gql World!"
      }
    }
    it("server should return Bad Request for invalid GET requests") {
      testApplication {
        application {
          graphQLModule()
        }
        val response = client.get("/graphql")
        response.status shouldBe HttpStatusCode.BadRequest
      }
    }

    it("server should return Method Not Allowed for Mutation GET requests") {
      testApplication {
        application {
          graphQLModule()
        }
        val response = client.get("/graphql") {
          parameter("query", "mutation { foo }")
        }
        response.status shouldBe HttpStatusCode.MethodNotAllowed
      }
    }

    it("server should handle valid POST requests") {
      testApplication {
        application {
          graphQLModule()

        }
        val client = createClient {
          install(ContentNegotiation) {
            jackson()
          }
        }
        val response = client.post("/graphql") {
          contentType(ContentType.Application.Json)
          setBody(GraphQLRequest(query = "query HelloWorldQuery { hello }"))
        }
        response.status shouldBe HttpStatusCode.OK
        response.bodyAsText() shouldContain """"hello":"Hello GraphQL!""""
      }
    }

    it("SDL route test") {
      val expectedSchema = """
        "Schema for Tally data"
        schema @contact(description : "Report issues on github.", name : "Tally", url : "https://github.com/cherba29/tally"){
          query: Query
        }
    
        "Provides contact information of the owner responsible for this subgraph schema."
        directive @contact(description: String!, name: String!, url: String!) on SCHEMA
    
        "Marks the field, argument, input field or enum value as deprecated"
        directive @deprecated(
            "The reason for the deprecation"
            reason: String = "No longer supported"
          ) on FIELD_DEFINITION | ARGUMENT_DEFINITION | ENUM_VALUE | INPUT_FIELD_DEFINITION
    
        "Directs the executor to include this field or fragment only when the `if` argument is true"
        directive @include(
            "Included when true."
            if: Boolean!
          ) on FIELD | FRAGMENT_SPREAD | INLINE_FRAGMENT
    
        "Indicates an Input Object is a OneOf Input Object."
        directive @oneOf on INPUT_OBJECT
    
        "Directs the executor to skip this field or fragment when the `if` argument is true."
        directive @skip(
            "Skipped when true."
            if: Boolean!
          ) on FIELD | FRAGMENT_SPREAD | INLINE_FRAGMENT
    
        "Exposes a URL that specifies the behaviour of this scalar."
        directive @specifiedBy(
            "The URL that specifies the behaviour of this scalar."
            url: String!
          ) on SCALAR
    
        type GqlAccount {
          address: String!
          "Month when account was closed. If not set means account is still open."
          closedOn: GqlMonth
          "Long description for the account."
          description: String!
          external: Boolean!
          "Account id/name."
          name: String!
          "Account number. Can be null or unknown for external or proxy accounts."
          number: String
          "Month when account was open. Can be unknown."
          openedOn: GqlMonth
          "List of owner ids for this account."
          owners: [String!]!
          password: String!
          "Replacement for type, so that accounts are grouped."
          path: [String!]!
          phone: String!
          summary: Boolean!
          "Account type. Determines how account is grouped."
          type: String!
          url: String!
          userName: String!
        }
        
        type GqlBalance {
          amount: Int!
          date: Date!
          type: String!
        }
      
        type GqlStatement {
          addSub: Int!
          annualizedPercentChange: Float!
          change: Int!
          endBalance: GqlBalance!
          hasProjectedTransfer: Boolean!
          inFlows: Int!
          income: Int!
          isClosed: Boolean!
          isCovered: Boolean!
          isProjectedCovered: Boolean!
          month: GqlMonth!
          name: String!
          outFlows: Int!
          percentChange: Float!
          startBalance: GqlBalance!
          totalPayments: Int!
          totalTransfers: Int!
          transactions: [GqlTransaction!]!
          unaccounted: Float!
        }
    
        type GqlSummaryData {
          statements: [GqlStatement!]!
          total: GqlSummaryStatement!
        }
  
        type GqlSummaryStatement {
          accounts: [String!]!
          addSub: Int!
          annualizedPercentChange: Float!
          change: Int!
          endBalance: GqlBalance!
          inFlows: Int!
          income: Int!
          month: GqlMonth!
          name: String!
          outFlows: Int!
          percentChange: Float!
          startBalance: GqlBalance!
          totalPayments: Int!
          totalTransfers: Int!
          unaccounted: Int!
        }
  
        type GqlTable {
          currentOwner: String!
          months: [GqlMonth!]!
          owners: [String!]!
          rows: [GqlTableRow!]!
        }
  
        type GqlTableCell {
          addSub: Int!
          annualizedPercentChange: Float!
          balance: Int!
          balanced: Boolean!
          hasProjectedTransfer: Boolean!
          isClosed: Boolean!
          isCovered: Boolean!
          isProjected: Boolean!
          isProjectedCovered: Boolean!
          month: GqlMonth!
          percentChange: Float!
          unaccounted: Int!
        }
      
        type GqlTableRow {
          account: GqlAccount!
          cells: [GqlTableCell!]!
          indent: Int!
          isNormal: Boolean!
          isSpace: Boolean!
          isTotal: Boolean!
          title: String!
        }
        
        type GqlTransaction {
          balance: GqlBalance!
          balanceFromStart: Int!
          description: String!
          isExpense: Boolean!
          isIncome: Boolean!
          toAccountName: String!
        }
  
        type Query {
          hello: String!
          "Returns a monthly statement for given account."
          statement(account: String!, month: String!, owner: String!): GqlStatement!
          "Generates delta summary table between two months."
          summary(endMonth: GqlMonth!, owner: String!, startMonth: GqlMonth!): GqlSummaryData!
          "Generates full tally table in given month range."
          table(endMonth: GqlMonth!, owner: String!, startMonth: GqlMonth!): GqlTable!
        }
        
        "Date representation in YYYY-MM-DD format."
        scalar Date
      
        "Month representation in XxxYYYY format."
        scalar GqlMonth
        """.trimIndent()
      testApplication {
        application {
          graphQLModule()
        }

        val response = client.get("/sdl")
        response.status shouldBe HttpStatusCode.OK
        response.bodyAsText().trim() shouldBe expectedSchema
      }
    }

    it("server should provide GraphiQL endpoint") {
      testApplication {
        application {
          graphQLModule()
        }

        val response = client.get("/graphiql")

        response.status shouldBe HttpStatusCode.OK

        val html = response.bodyAsText()
        html shouldContain "var serverUrl = '/graphql';"
        html shouldContain """var subscriptionUrl = new URL("/subscriptions", location.href);"""
      }
    }
  }
})