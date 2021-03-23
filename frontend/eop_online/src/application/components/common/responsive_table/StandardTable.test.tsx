import { render, within } from "@testing-library/react"
import React from "react"
import { KeyValues } from "./ResponsiveTable"
import StandardTable from "./StandardTable"
import { STANDARD_TABLE_CELL, STANDARD_TABLE_ROW } from "./testids"

describe("Standard table", () => {
    describe("transform data", () => {
        it("should render transformed data", () => {
            const data: KeyValues[] = [
                ["description", ["desc1", "desc2", "desc3"]],
                ["owner", ["owner1", "owner2"]],
            ]
            const element = render(<StandardTable data={data} />)
            const rows = element.getAllByTestId(STANDARD_TABLE_ROW)
            const entries = rows.map((row) => {
                const cells = within(row).getAllByTestId(STANDARD_TABLE_CELL)
                return cells.map((c) => c.textContent)
            })
            expect(entries).toStrictEqual([
                ["desc1", "owner1"],
                ["desc2", "owner2"],
                ["desc3", ""],
            ])
        })
    })
})
