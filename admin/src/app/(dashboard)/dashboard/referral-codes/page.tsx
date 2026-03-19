"use client";

import { useEffect, useState, useCallback } from "react";
import { api } from "@/lib/api";
import { mockApi } from "@/lib/mock-api";
import type { ReferralCode } from "@/types/api";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from "@/components/ui/dialog";
import { Skeleton } from "@/components/ui/skeleton";
import { EmptyState } from "@/components/empty-state";
import { Ticket, Plus, Pencil, Trash2 } from "lucide-react";
import { useActionResult } from "@/contexts/action-result-context";
import { Label } from "@/components/ui/label";

function toDatetimeLocal(iso: string): string {
  try {
    const d = new Date(iso);
    const pad = (n: number) => String(n).padStart(2, "0");
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  } catch {
    return "";
  }
}

function fromDatetimeLocal(local: string): string {
  if (!local) return "";
  return new Date(local).toISOString();
}

export default function ReferralCodesPage() {
  const { showSuccess, showError } = useActionResult();
  const [codes, setCodes] = useState<ReferralCode[] | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
  const [deleteId, setDeleteId] = useState<number | null>(null);
  const [saving, setSaving] = useState(false);

  const [formShortcode, setFormShortcode] = useState("");
  const [formShopName, setFormShopName] = useState("");
  const [formOfferPercent, setFormOfferPercent] = useState(10);
  const [formExpiresAt, setFormExpiresAt] = useState("");
  const [formActive, setFormActive] = useState(true);

  const loadCodes = useCallback(() => {
    setLoading(true);
    setError(null);
    if (mockApi.useMock()) {
      setCodes(mockApi.getMockReferralCodes());
      setLoading(false);
      return;
    }
    api.admin
      .getReferralCodes()
      .then(setCodes)
      .catch((e) => setError(e.message))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    loadCodes();
  }, [loadCodes]);

  const resetForm = () => {
    setFormShortcode("");
    setFormShopName("");
    setFormOfferPercent(10);
    const nextYear = new Date();
    nextYear.setFullYear(nextYear.getFullYear() + 1);
    setFormExpiresAt(toDatetimeLocal(nextYear.toISOString()));
    setFormActive(true);
  };

  const openCreate = () => {
    resetForm();
    setCreateOpen(true);
  };

  const openEdit = (row: ReferralCode) => {
    setFormShortcode(row.shortcode);
    setFormShopName(row.shopName);
    setFormOfferPercent(row.offerPercent);
    setFormExpiresAt(toDatetimeLocal(row.expiresAt));
    setFormActive(row.active);
    setEditId(row.id);
  };

  const handleCreate = async () => {
    if (!formShortcode.trim() || !formShopName.trim()) {
      showError("Validation", "Shortcode and shop name are required.");
      return;
    }
    const percent = Math.min(100, Math.max(0, Number(formOfferPercent) || 0));
    if (!formExpiresAt) {
      showError("Validation", "Expiration date is required.");
      return;
    }
    setSaving(true);
    try {
      await api.admin.createReferralCode({
        shortcode: formShortcode.trim().toUpperCase(),
        shopName: formShopName.trim(),
        offerPercent: percent,
        expiresAt: fromDatetimeLocal(formExpiresAt),
        active: formActive,
      });
      showSuccess("Referral code created", `${formShortcode.trim().toUpperCase()} has been created.`);
      setCreateOpen(false);
      loadCodes();
    } catch (e) {
      showError("Create failed", e instanceof Error ? e.message : "Failed to create referral code.");
    } finally {
      setSaving(false);
    }
  };

  const handleUpdate = async () => {
    if (editId == null || !formShortcode.trim() || !formShopName.trim()) return;
    const percent = Math.min(100, Math.max(0, Number(formOfferPercent) || 0));
    if (!formExpiresAt) {
      showError("Validation", "Expiration date is required.");
      return;
    }
    setSaving(true);
    try {
      await api.admin.updateReferralCode(editId, {
        shortcode: formShortcode.trim().toUpperCase(),
        shopName: formShopName.trim(),
        offerPercent: percent,
        expiresAt: fromDatetimeLocal(formExpiresAt),
        active: formActive,
      });
      showSuccess("Referral code updated", `${formShortcode.trim().toUpperCase()} has been updated.`);
      setEditId(null);
      loadCodes();
    } catch (e) {
      showError("Update failed", e instanceof Error ? e.message : "Failed to update referral code.");
    } finally {
      setSaving(false);
    }
  };

  const handleDelete = async () => {
    if (deleteId == null) return;
    setSaving(true);
    try {
      await api.admin.deleteReferralCode(deleteId);
      showSuccess("Referral code deleted", "The referral code has been removed.");
      setDeleteId(null);
      loadCodes();
    } catch (e) {
      showError("Delete failed", e instanceof Error ? e.message : "Failed to delete referral code.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-8">
      <div className="rounded-2xl border-2 border-border bg-gradient-to-r from-primary/5 via-primary/[0.03] to-transparent p-4 sm:p-6">
        <h1 className="page-header">Referral codes</h1>
        <p className="page-subheader">
          Manage subscription discount codes: shop name, shortcode (e.g. AMAZ5, SHOPSTOP10), offer percentage, and expiration.
        </p>
      </div>

      <Card className="border-2 border-border overflow-hidden">
        <CardHeader className="card-header-responsive border-b border-border/50 bg-muted/20 space-y-0">
          <CardTitle className="text-base font-bold flex items-center gap-2">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-tamixa-purple/15 text-tamixa-purple">
              <Ticket className="h-5 w-5" />
            </div>
            All referral codes
          </CardTitle>
          <Button onClick={openCreate} className="rounded-xl" size="sm">
            <Plus className="mr-2 h-4 w-4" />
            Add code
          </Button>
        </CardHeader>
        <CardContent className="p-0">
          {error && (
            <p className="px-6 pt-6 pb-2 text-sm font-medium text-destructive">{error}</p>
          )}
          {loading && !codes ? (
            <div className="p-6">
              <Skeleton className="h-48 w-full rounded-xl" />
            </div>
          ) : codes?.length === 0 ? (
            <div className="p-6">
              <EmptyState
                icon={Ticket}
                title="No referral codes"
                description="Create a code to offer subscription discounts (e.g. AMAZ5 for 5% off)."
                action={{ label: "Add referral code", onClick: openCreate }}
              />
            </div>
          ) : codes ? (
            <Table>
              <TableHeader>
                <TableRow className="hover:bg-transparent">
                  <TableHead className="font-semibold">Shortcode</TableHead>
                  <TableHead className="font-semibold">Shop name</TableHead>
                  <TableHead className="font-semibold">Offer %</TableHead>
                  <TableHead className="font-semibold">Expires</TableHead>
                  <TableHead className="font-semibold">Status</TableHead>
                  <TableHead className="font-semibold w-[120px]">Actions</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {codes.map((row) => (
                  <TableRow key={row.id} className="transition-colors hover:bg-muted/30">
                    <TableCell className="font-mono font-medium">{row.shortcode}</TableCell>
                    <TableCell>{row.shopName}</TableCell>
                    <TableCell>{row.offerPercent}%</TableCell>
                    <TableCell className="text-muted-foreground">
                      {new Date(row.expiresAt).toLocaleDateString(undefined, {
                        dateStyle: "medium",
                      })}
                    </TableCell>
                    <TableCell>
                      <Badge variant={row.active ? "default" : "secondary"} className="rounded-lg">
                        {row.active ? "Active" : "Inactive"}
                      </Badge>
                    </TableCell>
                    <TableCell>
                      <div className="flex gap-2">
                        <Button
                          variant="outline"
                          size="sm"
                          className="rounded-lg"
                          onClick={() => openEdit(row)}
                        >
                          <Pencil className="h-3 w-3" />
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          className="rounded-lg text-destructive hover:text-destructive"
                          onClick={() => setDeleteId(row.id)}
                        >
                          <Trash2 className="h-3 w-3" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          ) : null}
        </CardContent>
      </Card>

      {/* Create dialog */}
      <Dialog open={createOpen} onOpenChange={setCreateOpen}>
        <DialogContent className="sm:max-w-md rounded-2xl">
          <DialogHeader>
            <DialogTitle>New referral code</DialogTitle>
            <DialogDescription>
              Shortcode is shown to users (e.g. AMAZ5, SHOPSTOP10). Offer percent is the discount (0–100).
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid gap-2">
              <Label htmlFor="create-shortcode">Shortcode</Label>
              <Input
                id="create-shortcode"
                value={formShortcode}
                onChange={(e) => setFormShortcode(e.target.value.toUpperCase())}
                placeholder="AMAZ5"
                maxLength={32}
                className="rounded-xl font-mono"
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="create-shop">Shop name</Label>
              <Input
                id="create-shop"
                value={formShopName}
                onChange={(e) => setFormShopName(e.target.value)}
                placeholder="Amazon"
                maxLength={255}
                className="rounded-xl"
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="create-percent">Offer % (0–100)</Label>
              <Input
                id="create-percent"
                type="number"
                min={0}
                max={100}
                value={formOfferPercent}
                onChange={(e) => setFormOfferPercent(Number(e.target.value) || 0)}
                className="rounded-xl"
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="create-expires">Expires at</Label>
              <Input
                id="create-expires"
                type="datetime-local"
                value={formExpiresAt}
                onChange={(e) => setFormExpiresAt(e.target.value)}
                className="rounded-xl"
              />
            </div>
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="create-active"
                checked={formActive}
                onChange={(e) => setFormActive(e.target.checked)}
                className="rounded"
              />
              <Label htmlFor="create-active">Active</Label>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setCreateOpen(false)} className="rounded-xl">
              Cancel
            </Button>
            <Button onClick={handleCreate} disabled={saving} className="rounded-xl">
              {saving ? "Creating…" : "Create"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Edit dialog */}
      <Dialog open={editId != null} onOpenChange={(open) => !open && setEditId(null)}>
        <DialogContent className="sm:max-w-md rounded-2xl">
          <DialogHeader>
            <DialogTitle>Edit referral code</DialogTitle>
            <DialogDescription>
              Update shortcode, shop name, offer percent, or expiration.
            </DialogDescription>
          </DialogHeader>
          <div className="grid gap-4 py-4">
            <div className="grid gap-2">
              <Label htmlFor="edit-shortcode">Shortcode</Label>
              <Input
                id="edit-shortcode"
                value={formShortcode}
                onChange={(e) => setFormShortcode(e.target.value.toUpperCase())}
                placeholder="AMAZ5"
                maxLength={32}
                className="rounded-xl font-mono"
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="edit-shop">Shop name</Label>
              <Input
                id="edit-shop"
                value={formShopName}
                onChange={(e) => setFormShopName(e.target.value)}
                placeholder="Amazon"
                maxLength={255}
                className="rounded-xl"
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="edit-percent">Offer % (0–100)</Label>
              <Input
                id="edit-percent"
                type="number"
                min={0}
                max={100}
                value={formOfferPercent}
                onChange={(e) => setFormOfferPercent(Number(e.target.value) || 0)}
                className="rounded-xl"
              />
            </div>
            <div className="grid gap-2">
              <Label htmlFor="edit-expires">Expires at</Label>
              <Input
                id="edit-expires"
                type="datetime-local"
                value={formExpiresAt}
                onChange={(e) => setFormExpiresAt(e.target.value)}
                className="rounded-xl"
              />
            </div>
            <div className="flex items-center gap-2">
              <input
                type="checkbox"
                id="edit-active"
                checked={formActive}
                onChange={(e) => setFormActive(e.target.checked)}
                className="rounded"
              />
              <Label htmlFor="edit-active">Active</Label>
            </div>
          </div>
          <DialogFooter>
            <Button variant="outline" onClick={() => setEditId(null)} className="rounded-xl">
              Cancel
            </Button>
            <Button onClick={handleUpdate} disabled={saving} className="rounded-xl">
              {saving ? "Saving…" : "Save"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete confirm */}
      <Dialog open={deleteId != null} onOpenChange={(open) => !open && setDeleteId(null)}>
        <DialogContent className="sm:max-w-md rounded-2xl">
          <DialogHeader>
            <DialogTitle>Delete referral code</DialogTitle>
            <DialogDescription>
              This cannot be undone. The code will no longer apply at checkout.
            </DialogDescription>
          </DialogHeader>
          <DialogFooter>
            <Button variant="outline" onClick={() => setDeleteId(null)} className="rounded-xl">
              Cancel
            </Button>
            <Button variant="destructive" onClick={handleDelete} disabled={saving} className="rounded-xl">
              {saving ? "Deleting…" : "Delete"}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  );
}
