import { render, within } from "@testing-library/react"
import React from "react"
import CompactTable from "./CompactTable"
import { KeyValues } from "./ResponsiveTable"
import { COMPACT_TABLE_RECORD, COMPACT_TABLE_FIELD_HEADER, COMPACT_TABLE_FIELD_CONTENT } from "./testids"
import zip from "lodash/zip"

describe("Compact table", () => {
    describe("transform data", () => {
        it("should transform input data", () => {
            const data: KeyValues[] = [
                ["description", ["desc1", "desc2", "desc3"]],
                ["owner", ["owner1", "owner2"]],
            ]
            const element = render(<CompactTable data={data} />)
            const records = Array.from(element.getAllByTestId(COMPACT_TABLE_RECORD))
            const recordEntries = records.map((r) => {
                const headers = within(r)
                    .getAllByTestId(COMPACT_TABLE_FIELD_HEADER)
                    .map((e) => e.textContent)
                const contents = within(r)
                    .getAllByTestId(COMPACT_TABLE_FIELD_CONTENT)
                    .map((e) => e.textContent)
                return zip(headers, contents)
            })
            expect(recordEntries).toStrictEqual([
                [
                    ["description", "desc1"],
                    ["owner", "owner1"],
                ],
                [
                    ["description", "desc2"],
                    ["owner", "owner2"],
                ],
                [["description", "desc3"]],
            ])
        })
    })
})
